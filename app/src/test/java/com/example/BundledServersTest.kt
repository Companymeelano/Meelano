package com.example

import com.example.core.ConfigParser
import com.example.core.Protocol
import com.example.data.repository.BundledServers
import com.example.vpn.proto.OutboundFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the two promises made about the bundled VIP list:
 *  1. every link genuinely parses into something this build can connect to, and
 *  2. nothing anywhere in the UI can reveal the upstream provider.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundledServersTest {

    /** Substrings that must never reach the user interface. */
    private val secretMarkers = listOf(
        "hbsek", "aparat", "telewebion", "prMfS",
        "178.239.157.239", "37.32.44.59", "0sowk"
    )

    @Test
    fun `every vip link parses`() {
        assertTrue(BundledServers.vip.isNotEmpty())
        BundledServers.vip.forEach { server ->
            val endpoint = ConfigParser.parse(server.configLink)
            assertTrue("Unparseable: ${server.name}", endpoint != null)
            assertTrue("Invalid endpoint: ${server.name}", endpoint!!.isValid())
        }
    }

    @Test
    fun `every vip node uses a protocol this build can carry`() {
        BundledServers.vip.forEach { server ->
            val endpoint = ConfigParser.parse(server.configLink)!!
            assertTrue(
                "Unsupported protocol in ${server.name}",
                OutboundFactory.supports(endpoint)
            )
        }
    }

    @Test
    fun `vip nodes are vless over websocket tls`() {
        BundledServers.vip.forEach { server ->
            val endpoint = ConfigParser.parse(server.configLink)!!
            assertEquals(Protocol.VLESS, endpoint.protocol)
            assertEquals("ws", endpoint.network)
            assertEquals("tls", endpoint.security)
            assertTrue("Missing ws path in ${server.name}", endpoint.path.startsWith("/"))
            assertTrue("Missing SNI in ${server.name}", endpoint.effectiveSni.isNotBlank())
            assertTrue("Missing Host in ${server.name}", endpoint.effectiveHost.isNotBlank())
        }
    }

    @Test
    fun `displayed names never leak the upstream provider`() {
        BundledServers.vip.forEach { server ->
            val visible = listOf(server.name, server.countryName, server.hostLabel)
            visible.forEach { text ->
                secretMarkers.forEach { marker ->
                    assertFalse(
                        "\"$text\" leaks \"$marker\"",
                        text.contains(marker, ignoreCase = true)
                    )
                }
            }
        }
    }

    @Test
    fun `vip host label shows only country and protocol`() {
        val server = BundledServers.vip.first()
        val label = server.hostLabel
        assertTrue(label.contains(server.countryName))
        assertTrue(label.contains("VLESS"))
        // The real address must not be present in any form.
        assertFalse(label.contains(":"))
    }

    @Test
    fun `every vip node is branded meelano`() {
        BundledServers.vip.forEach { server ->
            assertTrue("Unbranded: ${server.name}", server.name.startsWith("MeeLano"))
            assertTrue(server.isVip)
        }
    }

    @Test
    fun `ids are unique and exactly one node is preselected`() {
        val ids = BundledServers.vip.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(1, BundledServers.vip.count { it.isSelected })
    }

    @Test
    fun `all four countries are represented`() {
        val countries = BundledServers.vip.map { it.countryName }.toSet()
        assertEquals(4, countries.size)
    }
}
