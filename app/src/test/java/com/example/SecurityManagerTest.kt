package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityManagerTest {

    private lateinit var manager: SecurityManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("meelano_security_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        manager = SecurityManager(context)
    }

    @Test
    fun `app name is branded`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("MeeLano Tunnel", context.getString(R.string.app_name))
    }

    @Test
    fun `correct default pin unlocks`() {
        manager.lock()
        assertTrue(manager.isLocked.value)
        "123".forEach { manager.appendPinDigit(it.toString()) }
        assertTrue(manager.isLocked.value)
        assertTrue(manager.appendPinDigit("4"))
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun `wrong pin keeps lock and reports error`() {
        manager.lock()
        "9999".forEach { manager.appendPinDigit(it.toString()) }
        assertTrue(manager.isLocked.value)
        assertNotNull(manager.pinError.value)
        assertEquals("", manager.currentPinInput.value)
    }

    @Test
    fun `changing pin invalidates the old one`() {
        assertTrue(manager.changePin("1234", "8642"))
        manager.lock()
        "1234".forEach { manager.appendPinDigit(it.toString()) }
        assertTrue(manager.isLocked.value)
        "8642".forEach { manager.appendPinDigit(it.toString()) }
        assertFalse(manager.isLocked.value)
    }

    @Test
    fun `too many failures triggers lockout`() {
        manager.lock()
        repeat(5) { attempt ->
            "0000".forEach { manager.appendPinDigit(it.toString()) }
        }
        assertTrue(manager.isLockedOut())
        // even a correct pin is refused during lockout
        "1234".forEach { manager.appendPinDigit(it.toString()) }
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun `delete removes last digit`() {
        manager.lock()
        manager.appendPinDigit("1")
        manager.appendPinDigit("2")
        manager.deletePinDigit()
        assertEquals("1", manager.currentPinInput.value)
    }
}
