package com.example

import com.example.core.ConfigParser
import com.example.vpn.net.TcpHeader
import com.example.vpn.stack.DnsMap
import com.example.vpn.stack.TcpStack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Covers the userspace TCP stack — the most intricate code in the project and,
 * until now, the least tested. These exercise the parts that can be driven
 * without a real network: flow bookkeeping, the RST path for unknown flows, and
 * the DNS-to-domain substitution that decides what address the proxy is asked
 * to dial.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TcpStackTest {

    private val written = CopyOnWriteArrayList<ByteArray>()
    private lateinit var stack: TcpStack

    private val clientIp = byteArrayOf(10, 0, 0, 2)
    private val serverIp = byteArrayOf(93.toByte(), 184.toByte(), 216.toByte(), 34)

    @Before
    fun setUp() {
        DnsMap.clear()
        written.clear()
        val endpoint = ConfigParser.parse(
            "vless://8d0f2761-c586-4822-8238-757ff717fdf8@127.0.0.1:1?encryption=none&type=tcp"
        )
        assertNotNull(endpoint)
        stack = TcpStack(
            endpoint = endpoint!!,
            protect = { true },
            writeToTun = { written.add(it) },
            log = {}
        )
    }

    @After
    fun tearDown() {
        stack.shutdown()
        DnsMap.clear()
    }

    @Test
    fun `a segment for an unknown flow is answered with a reset`() {
        // Without this the peer's socket hangs until its own timeout rather than
        // failing immediately, which users experience as the app freezing.
        val stray = segment(flags = TcpHeader.ACK, sequence = 500, ack = 900)
        stack.handlePacket(stray, stray.size)

        assertEquals(1, written.size)
        val reply = TcpHeader.parse(written[0], written[0].size)
        assertNotNull(reply)
        assertTrue("expected RST for an unknown flow", reply!!.isRst)

        // The reset must be addressed back to the sender, not echoed onward.
        assertEquals(TcpHeader.ip(serverIp), TcpHeader.ip(reply.sourceIp))
        assertEquals(TcpHeader.ip(clientIp), TcpHeader.ip(reply.destinationIp))
    }

    @Test
    fun `an incoming reset is not answered with another reset`() {
        // Replying to a RST with a RST is how packet storms start.
        val rst = segment(flags = TcpHeader.RST, sequence = 1, ack = 0)
        stack.handlePacket(rst, rst.size)
        assertTrue("a RST must not be answered", written.isEmpty())
    }

    @Test
    fun `a syn opens exactly one flow and is acknowledged`() {
        val syn = segment(flags = TcpHeader.SYN, sequence = 1000, ack = 0)
        stack.handlePacket(syn, syn.size)

        assertEquals(1L, stack.totalOpened)

        val reply = written.firstNotNullOfOrNull { TcpHeader.parse(it, it.size) }
        assertNotNull("the SYN should be answered immediately", reply)
        assertTrue(reply!!.isSyn && reply.isAck)
        // SYN-ACK must acknowledge exactly one past the client's sequence.
        assertEquals(1001L, reply.acknowledgement)
    }

    @Test
    fun `a repeated syn does not open a second flow`() {
        val syn = segment(flags = TcpHeader.SYN, sequence = 1000, ack = 0)
        stack.handlePacket(syn, syn.size)
        stack.handlePacket(syn, syn.size)

        // Retransmitted SYNs are normal on a lossy link; counting each as a new
        // flow would corrupt the health monitor's failure ratio.
        assertEquals(1L, stack.totalOpened)
    }

    @Test
    fun `a malformed packet is ignored rather than crashing the loop`() {
        val garbage = ByteArray(8) { 0xFF.toByte() }
        stack.handlePacket(garbage, garbage.size)
        assertEquals(0L, stack.totalOpened)
        assertTrue(written.isEmpty())
    }

    @Test
    fun `dns map returns the learned hostname for an address`() {
        // This substitution is what lets the exit node resolve the domain
        // itself, sidestepping poisoned DNS and landing on a live CDN edge.
        DnsMap.remember("Example.COM.", "93.184.216.34")
        assertEquals("example.com", DnsMap.hostFor("93.184.216.34"))
        assertNull(DnsMap.hostFor("1.2.3.4"))
    }

    @Test
    fun `dns map ignores blank entries and caps its growth`() {
        DnsMap.clear()
        DnsMap.remember("", "1.1.1.1")
        DnsMap.remember("host.example", "")
        assertEquals(0, DnsMap.size)

        // The cap exists so a long session cannot grow the map without bound.
        repeat(5000) { DnsMap.remember("h$it.example", "10.1.${it / 256}.${it % 256}") }
        assertTrue("map should stay bounded, was ${DnsMap.size}", DnsMap.size <= 4200)
    }

    // ---- helpers ----

    /** Builds a client-to-server segment for the flow under test. */
    private fun segment(flags: Int, sequence: Long, ack: Long): ByteArray =
        TcpHeader.build(
            sourceIp = clientIp,
            destinationIp = serverIp,
            sourcePort = 51_000,
            destinationPort = 443,
            sequence = sequence,
            acknowledgement = ack,
            flags = flags,
            window = 65_535
        )
}
