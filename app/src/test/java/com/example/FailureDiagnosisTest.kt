package com.example

import com.example.core.FailureDiagnosis
import com.example.vpn.HealthMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The message shown after a failure decides what the user does next, so the
 * classification is worth pinning down.
 */
class FailureDiagnosisTest {

    @Test
    fun `a reset right after connecting reads as blocking, not as a dead server`() {
        val result = FailureDiagnosis.diagnose("Connection reset by peer")
        assertEquals(FailureDiagnosis.Kind.BLOCKED, result.kind)
        assertTrue(result.tryAnotherServer)
        assertTrue(result.advice.isNotBlank())
    }

    @Test
    fun `a timeout is read differently depending on whether tcp got through`() {
        // Nothing answered: could be anything, including the user's own link.
        val cold = FailureDiagnosis.diagnose("timeout", tcpReachable = false)
        assertEquals(FailureDiagnosis.Kind.TIMEOUT, cold.kind)

        // The port answered and then went silent — that is DPI, not an outage,
        // and telling the two apart is the whole point of the tcpReachable flag.
        val warm = FailureDiagnosis.diagnose("timeout", tcpReachable = true)
        assertEquals(FailureDiagnosis.Kind.BLOCKED, warm.kind)
    }

    @Test
    fun `faults that repeat everywhere do not trigger failover`() {
        val vpn = FailureDiagnosis.diagnose("ایجاد رابط TUN ممکن نشد (مجوز VPN رد شد)")
        assertEquals(FailureDiagnosis.Kind.VPN_DENIED, vpn.kind)
        assertFalse("cycling servers cannot fix a denied VPN", vpn.tryAnotherServer)

        val dns = FailureDiagnosis.diagnose("Unable to resolve host")
        assertEquals(FailureDiagnosis.Kind.NO_INTERNET, dns.kind)
        assertFalse(dns.tryAnotherServer)
    }

    @Test
    fun `tls trust failures are explained as filtering rather than as an error code`() {
        val result = FailureDiagnosis.diagnose(
            "java.security.cert.CertPathValidatorException: Trust anchor not found"
        )
        assertEquals(FailureDiagnosis.Kind.TLS_REJECTED, result.kind)
        // The raw exception name must not survive into the user-facing text.
        assertFalse(result.summary.contains("CertPath"))
        assertTrue(result.tryAnotherServer)
    }

    @Test
    fun `an unknown failure still yields actionable advice`() {
        val result = FailureDiagnosis.diagnose("something entirely unexpected")
        assertEquals(FailureDiagnosis.Kind.UNKNOWN, result.kind)
        assertTrue(result.advice.isNotBlank())
    }

    @Test
    fun `a null cause does not produce an empty message`() {
        val result = FailureDiagnosis.diagnose(null)
        assertTrue(result.summary.isNotBlank())
        assertTrue(result.advice.isNotBlank())
    }

    @Test
    fun `health sample defaults let a flowless engine report on bytes alone`() {
        // The Xray core exposes no flow table; this is the shape it reports.
        val sample = HealthMonitor.Sample(opened = 0, failed = 0, bytesDown = 0, bytesUp = 50_000)
        assertEquals(0L, sample.opened)
        assertEquals(50_000L, sample.bytesUp)

        // bytesUp is optional so the Kotlin engine's existing call still builds.
        val legacy = HealthMonitor.Sample(opened = 10, failed = 1, bytesDown = 900)
        assertEquals(0L, legacy.bytesUp)
    }
}
