package com.example

import com.example.core.PingTester
import com.example.data.model.VpnServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Guards the sweep that appeared frozen at zero.
 *
 * The bug was not a wrong latency but a missing signal: [PingTester.pingAll]
 * reported nothing until every probe had finished, so the UI counter sat at 0
 * for the whole run and the app looked hung.
 */
class PingProgressTest {

    @Test
    fun `pingAll reports progress for every item`() {
        val open = ServerSocket(0)
        try {
            // Mix reachable and unreachable targets, which is the realistic case.
            val targets = listOf(
                "127.0.0.1" to open.localPort,
                "127.0.0.1" to open.localPort,
                "203.0.113.7" to 9,
                "127.0.0.1" to open.localPort
            )

            val seen = mutableListOf<Pair<Int, Int>>()
            val results = runBlocking {
                PingTester.pingAll(
                    items = targets,
                    parallelism = 2,
                    timeoutMs = 600,
                    keyOf = { "${it.first}:${it.second}#${targets.indexOf(it)}" },
                    addressOf = { it },
                    onProgress = { done, total -> synchronized(seen) { seen.add(done to total) } }
                )
            }

            // One callback per probe, and the final one must report completion.
            assertEquals(targets.size, seen.size)
            assertTrue(seen.all { it.second == targets.size })
            assertEquals(targets.size, seen.maxOf { it.first })
            // Progress must actually leave zero — the original defect.
            assertTrue("progress never advanced past 0", seen.any { it.first > 0 })
            assertTrue(results.isNotEmpty())
        } finally {
            open.close()
        }
    }

    @Test
    fun `pingAll still measures correctly with a slow member in the batch`() {
        val open = ServerSocket(0)
        try {
            // An unroutable address forces a timeout. With the old chunked()
            // implementation this stalled every worker in its batch.
            val targets = listOf(
                "127.0.0.1" to open.localPort,
                "203.0.113.9" to 9
            )

            val results = runBlocking {
                PingTester.pingAll(
                    items = targets,
                    parallelism = 4,
                    timeoutMs = 700,
                    keyOf = { "${it.first}:${it.second}" },
                    addressOf = { it }
                )
            }

            assertTrue(results.getValue("127.0.0.1:${open.localPort}") > 0)
            assertEquals(PingTester.UNREACHABLE, results.getValue("203.0.113.9:9"))
        } finally {
            open.close()
        }
    }

    @Test
    fun `a reachable server is not treated as verified until proven`() {
        val server = VpnServer(
            id = "x",
            name = "n",
            countryName = "c",
            flagEmoji = "f",
            protocol = "VLESS",
            isVip = true,
            pingMs = 42,
            speedMbps = 10f,
            configLink = "vless://u@h:443",
            lastTestedAt = System.currentTimeMillis()
        )

        // This is the exact trap that made dead VIP nodes look healthy: the port
        // answers, so latency looks great, but nothing has carried traffic.
        assertTrue(server.isReachable)
        assertFalse(server.isVerified)
        assertTrue(server.isUnproven)

        assertFalse(server.copy(isVerified = true).isUnproven)
    }
}
