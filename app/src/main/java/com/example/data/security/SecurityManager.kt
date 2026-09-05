package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * App lock with a salted SHA-256 PIN, brute-force lockout and real biometric
 * capability detection. The PIN is never stored in clear text and biometric
 * unlock only succeeds after the platform reports a successful authentication.
 */
class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meelano_security_prefs", Context.MODE_PRIVATE)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _currentPinInput = MutableStateFlow("")
    val currentPinInput: StateFlow<String> = _currentPinInput.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _lockoutUntil = MutableStateFlow(0L)
    val lockoutUntil: StateFlow<Long> = _lockoutUntil.asStateFlow()

    private val _hasPin = MutableStateFlow(prefs.contains(KEY_PIN_HASH))
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED, value).apply()

    init {
        if (!prefs.contains(KEY_PIN_HASH)) setPin(DEFAULT_PIN)
    }

    val biometricAvailable: Boolean
        get() = BiometricManager.from(context)
            .canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS

    fun lock() {
        _currentPinInput.value = ""
        _pinError.value = null
        _isLocked.value = true
    }

    fun unlock() {
        _currentPinInput.value = ""
        _pinError.value = null
        failedAttempts = 0
        _lockoutUntil.value = 0L
        _isLocked.value = false
    }

    /** Appends a digit; returns true when this completed a *correct* PIN. */
    fun appendPinDigit(digit: String): Boolean {
        if (isLockedOut()) {
            _pinError.value = lockoutMessage()
            return false
        }
        if (_currentPinInput.value.length >= PIN_LENGTH) return false

        val candidate = _currentPinInput.value + digit
        _currentPinInput.value = candidate
        _pinError.value = null
        if (candidate.length < PIN_LENGTH) return false

        return if (verifyPin(candidate)) {
            unlock()
            true
        } else {
            failedAttempts += 1
            _currentPinInput.value = ""
            if (failedAttempts >= MAX_ATTEMPTS) {
                _lockoutUntil.value = System.currentTimeMillis() + LOCKOUT_MS
                failedAttempts = 0
                _pinError.value = lockoutMessage()
            } else {
                _pinError.value = "پین اشتباه است (${MAX_ATTEMPTS - failedAttempts} تلاش باقی مانده)"
            }
            false
        }
    }

    fun deletePinDigit() {
        if (_currentPinInput.value.isNotEmpty()) {
            _currentPinInput.value = _currentPinInput.value.dropLast(1)
            _pinError.value = null
        }
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) {
            _pinError.value = "پین فعلی اشتباه است"
            return false
        }
        if (newPin.length != PIN_LENGTH || newPin.any { !it.isDigit() }) {
            _pinError.value = "پین جدید باید $PIN_LENGTH رقم باشد"
            return false
        }
        setPin(newPin)
        _pinError.value = null
        return true
    }

    fun onBiometricSucceeded() = unlock()

    fun onBiometricFailed(message: String?) {
        _pinError.value = message ?: "احراز هویت بیومتریک ناموفق بود"
    }

    fun isLockedOut(): Boolean = System.currentTimeMillis() < _lockoutUntil.value

    private fun lockoutMessage(): String {
        val seconds = ((_lockoutUntil.value - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        return "تلاش‌های زیاد ناموفق — $seconds ثانیه صبر کنید"
    }

    private fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return constantTimeEquals(hash(pin, salt), stored)
    }

    private fun setPin(pin: String) {
        val salt = newSalt()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .putInt(KEY_FAILED, 0)
            .apply()
        _hasPin.value = true
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(pin: String, salt: String): String {
        var digest = MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray())
        repeat(5_000) { digest = MessageDigest.getInstance("SHA-256").digest(digest) }
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        const val PIN_LENGTH = 4
        private const val DEFAULT_PIN = "1234"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 60_000L
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_SALT = "key_pin_salt"
        private const val KEY_FAILED = "key_failed_attempts"
    }
}
