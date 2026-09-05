package com.example

import com.example.core.ConfigParser
import com.example.core.PingTester
import com.example.vpn.TunnelEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * Guards the failure modes that left the UI stuck on "در حال اتصال".
 *
 * The bug these cover was not a wrong result but a missing one: a server that
 * accepts TCP and then says nothing produced no state transition at all, so the
 * app waited forever.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionRobustnessTest {

    /**
     * The central regression: a black-hole server must fail within the handshake
     * budget rather than hanging until the socket's own 60s read timeout.
     */
    @Test
    fun `silent server fails fast instead of hanging`() {
        // A listener that accepts connections and then deliberately never replies.
        val server = ServerSocket(0)
        val accepted = thread(start = true) {
            runCatching { server.accept() }
        }

        try {
            val link = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@" +
                "127.0.0.1:${server.localPort}?encryption=none&type=tcp"
            val endpoint = ConfigParser.parse(link)
            assertNotNull(endpoint)

            val started = System.currentTimeMillis()
            val result = runBlocking { TunnelEngine.handshake(endpoint!!) { true } }
            val elapsed = System.currentTimeMillis() - started

            assertFalse("A silent server must not report success", result.success)
            assertNotNull("A failure must explain itself", result.error)
            // The cap is 12s; allow generous slack for slow CI while still
            // proving we are nowhere near the old 60s socket timeout.
            assertTrue(
                "Handshake took ${elapsed}ms — the timeout did not fire",
                elapsed < 30_000
            )
        } finally {
            server.close()
            accepted.join(1_000)
        }
    }

    @Test
    fun `refused connection fails promptly`() {
        // Bind then immediately release, so the port is almost certainly closed.
        val port = ServerSocket(0).use { it.localPort }

        val endpoint = ConfigParser.parse(
            "vless://uuid-here@127.0.0.1:$port?encryption=none&type=tcp"
        )
        assertNotNull(endpoint)

        val started = System.currentTimeMillis()
        val result = runBlocking { TunnelEngine.handshake(endpoint!!) { true } }
        val elapsed = System.currentTimeMillis() - started

        assertFalse(result.success)
        assertTrue("Refusal should be quick, took ${elapsed}ms", elapsed < 30_000)
    }

    @Test
    fun `unsupported nodes are rejected without touching the network`() {
        val endpoint = ConfigParser.parse(
            "vless://uuid-here@192.0.2.1:443?type=quic&security=tls"
        )
        assertNotNull(endpoint)

        val started = System.currentTimeMillis()
        val result = runBlocking { TunnelEngine.handshake(endpoint!!) { true } }

        assertFalse(result.success)
        assertEquals(PingTester.UNREACHABLE, result.latencyMs)
        // No socket should be opened at all, so this must be effectively instant.
        assertTrue(System.currentTimeMillis() - started < 2_000)
    }

    private fun assertEquals(expected: Int, actual: Int) =
        org.junit.Assert.assertEquals(expected.toLong(), actual.toLong())
}
