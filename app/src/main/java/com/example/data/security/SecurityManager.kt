package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meelano_security_prefs", Context.MODE_PRIVATE)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _currentPinInput = MutableStateFlow("")
    val currentPinInput: StateFlow<String> = _currentPinInput.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(
        prefs.getBoolean("key_biometric_enabled", true)
    )
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Default PIN: 1234 as per requirements
    private var savedPin: String
        get() = prefs.getString("key_passcode_pin", "1234") ?: "1234"
        set(value) = prefs.edit().putString("key_passcode_pin", value).apply()

    fun lock() {
        _currentPinInput.value = ""
        _pinError.value = null
        _isLocked.value = true
    }

    fun unlock() {
        _currentPinInput.value = ""
        _pinError.value = null
        _isLocked.value = false
    }

    fun appendPinDigit(digit: String): Boolean {
        if (_currentPinInput.value.length >= 4) return false
        val newPin = _currentPinInput.value + digit
        _currentPinInput.value = newPin
        _pinError.value = null

        if (newPin.length == 4) {
            if (newPin == savedPin) {
                unlock()
                return true
            } else {
                _pinError.value = "پین‌کد وارد شده اشتباه است!"
                _currentPinInput.value = ""
                return false
            }
        }
        return false
    }

    fun deletePinDigit() {
        if (_currentPinInput.value.isNotEmpty()) {
            _currentPinInput.value = _currentPinInput.value.dropLast(1)
            _pinError.value = null
        }
    }

    fun authenticateWithCredentials(username: String, pass: String): Boolean {
        // VIP Account Authentication validation
        if (username.isNotBlank() && pass.length >= 4) {
            unlock()
            return true
        }
        _pinError.value = "نام کاربری یا رمز عبور نامعتبر است"
        return false
    }

    fun authenticateWithBiometricSuccess() {
        unlock()
    }
}
