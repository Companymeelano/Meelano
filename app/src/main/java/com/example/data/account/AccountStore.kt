package com.example.data.account

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local account database, sign-in, and live quota accounting.
 *
 * Everything here is on-device. That is an honest limitation worth stating: a
 * determined owner of the handset can clear app data and get a fresh guest
 * session. It gates who may use *this installation* and tracks their allowance;
 * it is not server-enforced entitlement, which would need a backend to be
 * meaningful. Passwords are still hashed rather than stored, so a leaked backup
 * does not hand over credentials that users may have reused elsewhere.
 */
class AccountStore(context: Context) {

    private val prefs = context.getSharedPreferences("meelano_accounts", Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _session = MutableStateFlow(Session())
    val session: StateFlow<Session> = _session.asStateFlow()

    /** Set when a session ends because time or traffic ran out. */
    private val _lockNotice = MutableStateFlow(LockReason.NONE)
    val lockNotice: StateFlow<LockReason> = _lockNotice.asStateFlow()

    init {
        _accounts.value = load()
        if (_accounts.value.none { it.role == Role.ADMIN }) {
            // Bootstrap: without a first administrator there is no way to create
            // any account at all. Credentials are surfaced in the admin panel and
            // the password can be changed there.
            createAccountInternal(
                username = DEFAULT_ADMIN_USER,
                password = DEFAULT_ADMIN_PASS,
                role = Role.ADMIN,
                quotaBytes = Account.UNLIMITED,
                expiresAt = Account.UNLIMITED,
                note = "حساب مدیر پیش‌فرض"
            )
        }
        restoreSession()
    }

    // region authentication

    /**
     * Verifies credentials and starts a session.
     *
     * Deliberately returns the same failure for an unknown username and a wrong
     * password, so the dialog cannot be used to enumerate who has an account.
     */
    fun signIn(username: String, password: String): SignInResult {
        val account = _accounts.value.firstOrNull {
            it.username.equals(username.trim(), ignoreCase = true)
        } ?: return SignInResult.InvalidCredentials

        if (hash(password, account.salt) != account.passwordHash) {
            return SignInResult.InvalidCredentials
        }
        if (account.suspended) return SignInResult.Locked(LockReason.SUSPENDED)

        // Report an exhausted account at the door rather than letting the user
        // in only to have every connection attempt refused silently.
        val reason = account.lockReason()
        val updated = account.copy(lastLoginAt = System.currentTimeMillis())
        upsert(updated)
        _session.value = Session(account = updated, isGuest = false)
        prefs.edit().putString(KEY_SESSION, updated.username).apply()

        return if (reason == LockReason.NONE) SignInResult.Success(updated)
        else SignInResult.SignedInButLocked(updated, reason)
    }

    fun continueAsGuest() {
        _session.value = Session(account = null, isGuest = true)
        prefs.edit().putString(KEY_SESSION, GUEST_MARKER).apply()
    }

    fun signOut() {
        _session.value = Session()
        _lockNotice.value = LockReason.NONE
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private fun restoreSession() {
        when (val saved = prefs.getString(KEY_SESSION, null)) {
            null -> Unit
            GUEST_MARKER -> _session.value = Session(isGuest = true)
            else -> _accounts.value.firstOrNull { it.username == saved }?.let {
                _session.value = Session(account = it)
                // Surface an allowance that ran out while the app was closed.
                val reason = it.lockReason()
                if (reason != LockReason.NONE) _lockNotice.value = reason
            }
        }
    }

    // endregion

    // region administration

    /** Creates an account. Fails if the username is taken or the input is thin. */
    fun createAccount(
        username: String,
        password: String,
        role: Role = Role.USER,
        quotaBytes: Long = Account.UNLIMITED,
        durationDays: Int = -1,
        note: String = ""
    ): Result<Account> {
        val name = username.trim()
        if (name.length < 3) return Result.failure(IllegalArgumentException("نام کاربری حداقل ۳ نویسه"))
        if (password.length < 4) return Result.failure(IllegalArgumentException("گذرواژه حداقل ۴ نویسه"))
        if (_accounts.value.any { it.username.equals(name, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("این نام کاربری قبلاً ثبت شده"))
        }
        val expiresAt = if (durationDays <= 0) Account.UNLIMITED
        else System.currentTimeMillis() + durationDays * DAY_MILLIS

        return Result.success(
            createAccountInternal(name, password, role, quotaBytes, expiresAt, note)
        )
    }

    private fun createAccountInternal(
        username: String,
        password: String,
        role: Role,
        quotaBytes: Long,
        expiresAt: Long,
        note: String
    ): Account {
        val salt = newSalt()
        val account = Account(
            username = username,
            passwordHash = hash(password, salt),
            salt = salt,
            role = role,
            quotaBytes = quotaBytes,
            expiresAt = expiresAt,
            note = note
        )
        upsert(account)
        return account
    }

    fun deleteAccount(username: String): Result<Unit> {
        val target = _accounts.value.firstOrNull { it.username == username }
            ?: return Result.failure(IllegalArgumentException("حساب پیدا نشد"))
        // Refuse to remove the last administrator: doing so would make the panel
        // permanently unreachable.
        if (target.role == Role.ADMIN && _accounts.value.count { it.role == Role.ADMIN } <= 1) {
            return Result.failure(IllegalArgumentException("آخرین حساب مدیر حذف نمی‌شود"))
        }
        persist(_accounts.value.filterNot { it.username == username })
        if (_session.value.account?.username == username) signOut()
        return Result.success(Unit)
    }

    fun setSuspended(username: String, suspended: Boolean) {
        _accounts.value.firstOrNull { it.username == username }?.let {
            upsert(it.copy(suspended = suspended))
        }
    }

    /** Grants more traffic and/or more days, and clears the exhausted state. */
    fun renew(username: String, addBytes: Long, addDays: Int) {
        val account = _accounts.value.firstOrNull { it.username == username } ?: return
        val now = System.currentTimeMillis()
        val base = if (account.hasExpiry && account.expiresAt > now) account.expiresAt else now
        upsert(
            account.copy(
                quotaBytes = when {
                    addBytes <= 0 -> account.quotaBytes
                    !account.hasQuotaLimit -> Account.UNLIMITED
                    else -> account.quotaBytes + addBytes
                },
                expiresAt = if (addDays <= 0) account.expiresAt else base + addDays * DAY_MILLIS
            )
        )
    }

    fun setQuota(username: String, quotaBytes: Long) {
        _accounts.value.firstOrNull { it.username == username }?.let {
            upsert(it.copy(quotaBytes = quotaBytes))
        }
    }

    fun setExpiry(username: String, expiresAt: Long) {
        _accounts.value.firstOrNull { it.username == username }?.let {
            upsert(it.copy(expiresAt = expiresAt))
        }
    }

    fun resetUsage(username: String) {
        _accounts.value.firstOrNull { it.username == username }?.let {
            upsert(it.copy(usedBytes = 0L))
        }
    }

    fun changePassword(username: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("گذرواژه حداقل ۴ نویسه"))
        }
        val account = _accounts.value.firstOrNull { it.username == username }
            ?: return Result.failure(IllegalArgumentException("حساب پیدا نشد"))
        val salt = newSalt()
        upsert(account.copy(salt = salt, passwordHash = hash(newPassword, salt)))
        return Result.success(Unit)
    }

    // endregion

    // region usage accounting

    /**
     * Adds traffic to the signed-in account and reports whether the tunnel must
     * now stop.
     *
     * Called from the VPN service as counters advance, so the ceiling is applied
     * while traffic is flowing rather than at the next sign-in.
     *
     * @return the reason to disconnect, or [LockReason.NONE] to keep running.
     */
    fun recordUsage(deltaBytes: Long): LockReason {
        if (deltaBytes <= 0) return LockReason.NONE
        val current = _session.value.account ?: return LockReason.NONE
        if (current.role == Role.ADMIN) return LockReason.NONE

        val updated = current.copy(usedBytes = current.usedBytes + deltaBytes)
        upsert(updated)
        _session.value = _session.value.copy(account = updated)

        val reason = updated.lockReason()
        if (reason != LockReason.NONE) _lockNotice.value = reason
        return reason
    }

    /** Re-checks expiry for the live session; call periodically while connected. */
    fun refreshLock(): LockReason {
        val account = _session.value.account ?: return LockReason.NONE
        val reason = account.lockReason()
        if (reason != LockReason.NONE) _lockNotice.value = reason
        return reason
    }

    fun acknowledgeLockNotice() {
        _lockNotice.value = LockReason.NONE
    }

    // endregion

    // region persistence

    private fun upsert(account: Account) {
        val others = _accounts.value.filterNot { it.username == account.username }
        persist(others + account)
        if (_session.value.account?.username == account.username) {
            _session.value = _session.value.copy(account = account)
        }
    }

    private fun persist(list: List<Account>) {
        val sorted = list.sortedWith(compareByDescending<Account> { it.role == Role.ADMIN }
            .thenBy { it.username.lowercase() })
        _accounts.value = sorted
        val array = JSONArray()
        sorted.forEach { account ->
            array.put(
                JSONObject()
                    .put("username", account.username)
                    .put("passwordHash", account.passwordHash)
                    .put("salt", account.salt)
                    .put("role", account.role.name)
                    .put("quotaBytes", account.quotaBytes)
                    .put("usedBytes", account.usedBytes)
                    .put("expiresAt", account.expiresAt)
                    .put("createdAt", account.createdAt)
                    .put("lastLoginAt", account.lastLoginAt)
                    .put("suspended", account.suspended)
                    .put("note", account.note)
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    }

    private fun load(): List<Account> = runCatching {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val o = array.getJSONObject(index)
                Account(
                    username = o.getString("username"),
                    passwordHash = o.getString("passwordHash"),
                    salt = o.getString("salt"),
                    role = runCatching { Role.valueOf(o.optString("role", "USER")) }
                        .getOrDefault(Role.USER),
                    quotaBytes = o.optLong("quotaBytes", Account.UNLIMITED),
                    usedBytes = o.optLong("usedBytes", 0L),
                    expiresAt = o.optLong("expiresAt", Account.UNLIMITED),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    lastLoginAt = o.optLong("lastLoginAt", 0L),
                    suspended = o.optBoolean("suspended", false),
                    note = o.optString("note", "")
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    // endregion

    // region crypto

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * PBKDF2-HMAC-SHA256. Slow by design: a plain SHA-256 of a four-digit
     * password falls to a wordlist instantly.
     */
    private fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            ITERATIONS,
            256
        )
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    // endregion

    companion object {
        const val DEFAULT_ADMIN_USER = "admin"
        const val DEFAULT_ADMIN_PASS = "meelano2024"

        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_SESSION = "session"
        private const val GUEST_MARKER = "__guest__"
        private const val ITERATIONS = 10_000
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val GIGABYTE = 1024L * 1024 * 1024
    }
}

/** Outcome of a sign-in attempt. */
sealed interface SignInResult {
    data class Success(val account: Account) : SignInResult

    /** Credentials were right but the allowance is gone; show the warning. */
    data class SignedInButLocked(val account: Account, val reason: LockReason) : SignInResult

    data class Locked(val reason: LockReason) : SignInResult
    data object InvalidCredentials : SignInResult
}
