package com.example.vpn.net

import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong

/**
 * A real DNS resolver running inside the tunnel.
 *
 * DNS queries that the OS writes into the TUN device are picked up here, sent to
 * the configured upstream resolver over a *protected* socket (so they leave the
 * device outside the tunnel loop), and the answer is written back into TUN as a
 * properly checksummed IPv4/UDP packet. This is what makes name resolution work
 * while the VPN interface owns the default route.
 */
class DnsRelay(
    private val upstreamServers: List<String>,
    private val protect: (DatagramSocket) -> Boolean,
    private val onLog: (String) -> Unit = {}
) {
    val queriesHandled = AtomicLong(0)
    val bytesIn = AtomicLong(0)
    val bytesOut = AtomicLong(0)

    /** Domains that must never be tunnelled (resolved but noted for direct routing). */
    private val blockedSuffixes = listOf(
        "doubleclick.net", "googleadservices.com", "adservice.google.com"
    )

    fun handle(request: IpPacket.Udp, tunOutput: FileOutputStream): Boolean {
        if (request.destinationPort != 53) return false
        val domain = extractQueryName(request.payload)
        if (domain != null && blockedSuffixes.any { domain.endsWith(it) }) {
            // Answer with NXDOMAIN-like empty response: cheap, effective ad blocking.
            val refused = buildRefusal(request.payload) ?: return false
            tunOutput.write(IpPacket.buildUdpResponse(request, refused))
            queriesHandled.incrementAndGet()
            return true
        }

        for (server in upstreamServers) {
            val answer = resolve(server, request.payload) ?: continue
            // Learn domain -> IP so proxied TCP flows can be dialled by hostname.
            if (domain != null) learnAnswers(domain, answer)
            tunOutput.write(IpPacket.buildUdpResponse(request, answer))
            queriesHandled.incrementAndGet()
            bytesOut.addAndGet(request.payload.size.toLong())
            bytesIn.addAndGet(answer.size.toLong())
            return true
        }
        onLog("DNS: no upstream answered for ${domain ?: "query"}")
        return false
    }

    private fun resolve(server: String, query: ByteArray): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket().apply {
                protect(this)
                soTimeout = 4000
            }
            val address = InetSocketAddress(InetAddress.getByName(server), 53)
            socket.send(DatagramPacket(query, query.size, address))
            val buffer = ByteArray(1500)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            buffer.copyOf(response.length)
        } catch (_: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /** Reads the QNAME out of a DNS query so we can log/filter by domain. */
    fun extractQueryName(query: ByteArray): String? {
        if (query.size < 13) return null
        return try {
            val sb = StringBuilder()
            var index = 12
            while (index < query.size) {
                val length = query[index].toInt() and 0xFF
                if (length == 0) break
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(String(query, index + 1, length, Charsets.US_ASCII))
                index += length + 1
            }
            sb.toString().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Walks the answer section of a DNS response and records every A record, so
     * [com.example.vpn.stack.DnsMap] can later turn a destination IP back into
     * the hostname the app originally asked for.
     */
    fun learnAnswers(domain: String, response: ByteArray) {
        try {
            if (response.size < 12) return
            val questions = IpPacket.readUShort(response, 4)
            val answers = IpPacket.readUShort(response, 6)
            if (answers <= 0) return

            var index = 12
            // Skip the question section.
            repeat(questions) {
                index = skipName(response, index)
                index += 4                        // QTYPE + QCLASS
            }

            repeat(answers) {
                if (index + 10 > response.size) return
                index = skipName(response, index)
                if (index + 10 > response.size) return
                val type = IpPacket.readUShort(response, index)
                val dataLength = IpPacket.readUShort(response, index + 8)
                index += 10
                if (index + dataLength > response.size) return
                if (type == 1 && dataLength == 4) {
                    val ip = (0 until 4).joinToString(".") {
                        (response[index + it].toInt() and 0xFF).toString()
                    }
                    com.example.vpn.stack.DnsMap.remember(domain, ip)
                }
                index += dataLength
            }
        } catch (_: Exception) {
            // A malformed answer must never break resolution.
        }
    }

    /** Skips a (possibly compressed) DNS name and returns the following offset. */
    private fun skipName(data: ByteArray, start: Int): Int {
        var index = start
        while (index < data.size) {
            val length = data[index].toInt() and 0xFF
            when {
                length == 0 -> return index + 1
                length and 0xC0 == 0xC0 -> return index + 2   // compression pointer
                else -> index += length + 1
            }
        }
        return index
    }

    /** Builds a NAME-ERROR (RCODE 3) response for the given query. */
    private fun buildRefusal(query: ByteArray): ByteArray? {
        if (query.size < 12) return null
        val response = query.copyOf()
        response[2] = (response[2].toInt() or 0x80).toByte()      // QR = response
        response[3] = ((response[3].toInt() and 0xF0) or 0x03).toByte() // RCODE = NXDOMAIN
        return response
    }
}
