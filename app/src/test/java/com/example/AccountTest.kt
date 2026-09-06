package com.example

import com.example.data.account.Account
import com.example.data.account.LockReason
import com.example.data.account.Quota
import com.example.data.account.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The entitlement rules.
 *
 * These decide when a paying user is cut off, so an error here either strands
 * someone who still has credit or hands out service that has already run out.
 */
class AccountTest {

    private fun account(
        quota: Long = Account.UNLIMITED,
        used: Long = 0,
        expires: Long = Account.UNLIMITED,
        role: Role = Role.USER,
        suspended: Boolean = false
    ) = Account(
        username = "u",
        passwordHash = "h",
        salt = "s",
        role = role,
        quotaBytes = quota,
        usedBytes = used,
        expiresAt = expires,
        createdAt = 0L,
        suspended = suspended
    )

    @Test
    fun `unlimited account is never locked`() {
        val a = account()
        assertTrue(a.canConnect())
        assertEquals(LockReason.NONE, a.lockReason())
        assertEquals(Long.MAX_VALUE, a.remainingBytes)
    }

    @Test
    fun `quota exhaustion locks the account`() {
        val a = account(quota = 100, used = 100)
        assertFalse(a.canConnect())
        assertEquals(LockReason.QUOTA_EXHAUSTED, a.lockReason())
        assertEquals(0L, a.remainingBytes)
    }

    @Test
    fun `usage beyond the ceiling never reports negative remaining`() {
        // The service bills in chunks, so the final chunk usually overshoots.
        val a = account(quota = 100, used = 250)
        assertEquals(0L, a.remainingBytes)
        assertEquals(1f, a.quotaFraction, 0.001f)
    }

    @Test
    fun `expiry locks the account`() {
        val now = 10_000L
        val a = account(expires = 5_000L)
        assertEquals(LockReason.EXPIRED, a.lockReason(now))
        assertEquals(0L, a.remainingMillis(now))
    }

    @Test
    fun `expiry in the future leaves the account usable`() {
        val now = 1_000L
        val a = account(expires = 9_000L)
        assertEquals(LockReason.NONE, a.lockReason(now))
        assertEquals(8_000L, a.remainingMillis(now))
    }

    @Test
    fun `suspension outranks everything`() {
        val a = account(suspended = true)
        assertEquals(LockReason.SUSPENDED, a.lockReason())
    }

    @Test
    fun `admins are exempt from quota and expiry`() {
        // Locking out the only administrator would leave nobody able to renew
        // anyone else's account.
        val a = account(quota = 10, used = 999, expires = 1L, role = Role.ADMIN)
        assertEquals(LockReason.NONE, a.lockReason(now = 500_000L))
        assertTrue(a.canConnect(now = 500_000L))
    }

    @Test
    fun `quota fraction tracks consumption`() {
        assertEquals(0.5f, account(quota = 100, used = 50).quotaFraction, 0.001f)
        assertEquals(0f, account().quotaFraction, 0.001f)
    }

    @Test
    fun `time fraction spans creation to expiry`() {
        val a = account(expires = 100L)
        assertEquals(0.25f, a.timeFraction(now = 25L), 0.001f)
        assertEquals(1f, a.timeFraction(now = 200L), 0.001f)
    }

    @Test
    fun `formatting produces persian digits and units`() {
        assertEquals("نامحدود", Quota.bytes(Account.UNLIMITED))
        assertEquals("پایان یافته", Quota.duration(0))
        assertEquals("نامحدود", Quota.duration(Long.MAX_VALUE))
        // 2 GiB, and the digits must not be ASCII.
        val twoGb = Quota.bytes(2L * 1024 * 1024 * 1024)
        assertTrue(twoGb, twoGb.contains("گیگابایت"))
        assertFalse("ASCII digits leaked into the UI string", twoGb.any { it in '0'..'9' })
    }

    @Test
    fun `lock messages are populated for every real reason`() {
        listOf(LockReason.EXPIRED, LockReason.QUOTA_EXHAUSTED, LockReason.SUSPENDED)
            .forEach { reason ->
                assertTrue(Quota.lockMessage(reason).isNotBlank())
                assertTrue(Quota.lockTitle(reason).isNotBlank())
            }
        assertEquals("", Quota.lockMessage(LockReason.NONE))
    }
}
