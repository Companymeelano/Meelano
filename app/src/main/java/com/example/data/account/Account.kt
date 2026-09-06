package com.example.data.account

/** Who a signed-in principal is allowed to be. */
enum class Role {
    /** No credentials. Free servers only. */
    GUEST,

    /** A provisioned account. Full access while quota and time remain. */
    USER,

    /** Can create, edit, suspend and delete accounts. */
    ADMIN
}

/**
 * Why an account is not currently allowed to connect.
 *
 * Kept as a type rather than a boolean so the UI can explain the reason in the
 * user's own words instead of a generic refusal.
 */
enum class LockReason {
    NONE,
    EXPIRED,
    QUOTA_EXHAUSTED,
    SUSPENDED
}

/**
 * A user of the app.
 *
 * Quota and expiry are stored as absolutes — a byte ceiling and a wall-clock
 * deadline — with usage accumulated alongside. Storing "days remaining" instead
 * would drift every time the device clock moved.
 *
 * @param passwordHash PBKDF2 hash; the plaintext password is never persisted.
 * @param quotaBytes total allowance, or [UNLIMITED] for no ceiling.
 * @param usedBytes traffic consumed so far, carried across sessions.
 * @param expiresAt epoch millis after which the account stops working, or
 *   [UNLIMITED] to never expire.
 */
data class Account(
    val username: String,
    val passwordHash: String,
    val salt: String,
    val role: Role = Role.USER,
    val quotaBytes: Long = UNLIMITED,
    val usedBytes: Long = 0L,
    val expiresAt: Long = UNLIMITED,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L,
    val suspended: Boolean = false,
    val note: String = ""
) {
    val hasQuotaLimit: Boolean get() = quotaBytes != UNLIMITED
    val hasExpiry: Boolean get() = expiresAt != UNLIMITED

    /** Remaining traffic in bytes; [Long.MAX_VALUE] when unlimited. */
    val remainingBytes: Long
        get() = if (!hasQuotaLimit) Long.MAX_VALUE else (quotaBytes - usedBytes).coerceAtLeast(0L)

    /** Remaining time in millis; [Long.MAX_VALUE] when unlimited. */
    fun remainingMillis(now: Long = System.currentTimeMillis()): Long =
        if (!hasExpiry) Long.MAX_VALUE else (expiresAt - now).coerceAtLeast(0L)

    /** 0f..1f share of the quota consumed. 0 when unlimited. */
    val quotaFraction: Float
        get() = if (!hasQuotaLimit || quotaBytes <= 0) 0f
        else (usedBytes.toFloat() / quotaBytes).coerceIn(0f, 1f)

    /** 0f..1f share of the subscription period elapsed. 0 when unlimited. */
    fun timeFraction(now: Long = System.currentTimeMillis()): Float {
        if (!hasExpiry) return 0f
        val span = expiresAt - createdAt
        if (span <= 0) return 1f
        return ((now - createdAt).toFloat() / span).coerceIn(0f, 1f)
    }

    /**
     * Whether this account may open a tunnel, and if not, why.
     *
     * Admins are deliberately exempt from quota and expiry: locking the only
     * administrator out of the panel would leave nobody able to renew anyone.
     */
    fun lockReason(now: Long = System.currentTimeMillis()): LockReason = when {
        suspended -> LockReason.SUSPENDED
        role == Role.ADMIN -> LockReason.NONE
        hasExpiry && now >= expiresAt -> LockReason.EXPIRED
        hasQuotaLimit && usedBytes >= quotaBytes -> LockReason.QUOTA_EXHAUSTED
        else -> LockReason.NONE
    }

    fun canConnect(now: Long = System.currentTimeMillis()): Boolean =
        lockReason(now) == LockReason.NONE

    companion object {
        /** Sentinel for "no ceiling" on both quota and expiry. */
        const val UNLIMITED = -1L
    }
}

/** The active principal: either a signed-in [Account] or an anonymous guest. */
data class Session(
    val account: Account? = null,
    val isGuest: Boolean = false
) {
    val isSignedIn: Boolean get() = account != null
    val role: Role get() = account?.role ?: Role.GUEST
    val isAdmin: Boolean get() = role == Role.ADMIN

    /** Guests see only the free list; VIP nodes need a real account. */
    val canUseVip: Boolean get() = account != null && account.canConnect()

    val displayName: String get() = account?.username ?: "مهمان"
}
