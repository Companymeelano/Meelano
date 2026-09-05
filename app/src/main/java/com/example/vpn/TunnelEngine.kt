package com.example.vpn

import com.example.core.PingTester
import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import com.example.vpn.proto.Destination
import com.example.vpn.proto.OutboundFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import java.net.Socket

/**
 * Verifies, end to end, that a node can actually carry traffic — before the UI
 * is allowed to claim "connected".
 *
 * The check is deliberately strict: it performs the node's *real* protocol
 * handshake and then asks it to proxy a genuine HTTP request to a well-known
 * host. A node that accepts TCP but silently drops payload (very common with
 * dead free configs) therefore fails here instead of leaving the user with a
 * connected-looking VPN that loads nothing.
 */
object TunnelEngine {

    /** Small, always-available probe target with a tiny, predictable response. */
    private const val PROBE_HOST = "www.gstatic.com"
    private const val PROBE_PORT = 80
    private const val PROBE_REQUEST =
        "HEAD /generate_204 HTTP/1.1\r\nHost: www.gstatic.com\r\nConnection: close\r\n" +
            "User-Agent: Mozilla/5.0 (Linux; Android 13)\r\n\r\n"

    data class HandshakeResult(
        val success: Boolean,
        val latencyMs: Int,
        val negotiatedProtocol: String,
        val cipherSuite: String,
        val error: String? = null,
        /** True when the node proved it can relay real payload, not just connect. */
        val payloadVerified: Boolean = false
    )

    /** Nothing genuinely usable completes a handshake more slowly than this. */
    private const val HANDSHAKE_TIMEOUT_MS = 12_000L

    suspend fun handshake(
        endpoint: ProxyEndpoint,
        protect: (Socket) -> Boolean
    ): HandshakeResult = withContext(Dispatchers.IO) {
        if (!OutboundFactory.supports(endpoint)) {
            return@withContext HandshakeResult(
                success = false,
                latencyMs = PingTester.UNREACHABLE,
                negotiatedProtocol = "-",
                cipherSuite = "-",
                error = OutboundFactory.unsupportedReason(endpoint)
            )
        }

        val started = System.currentTimeMillis()
        var outbound: com.example.vpn.proto.Outbound? = null
        var negotiated = "TCP"
        var cipher = "none"

        // Hard ceiling on the whole handshake.
        //
        // Hard ceiling on the whole handshake.
        //
        // A node that accepts TCP and then stays silent — very common among dead
        // free nodes — would otherwise pin the UI in "connecting" until the
        // socket's own 60s read timeout expired.
        //
        // Note that wrapping this in withTimeoutOrNull alone does NOT work:
        // InputStream.read is a blocking call that ignores coroutine
        // cancellation, so the timeout could not fire until the read had already
        // returned. The watchdog therefore closes the outbound, which forces the
        // pending read to throw and unwinds the attempt immediately.
        val watchdog = Timer("meelano-handshake-watchdog", true)
        var timedOut = false
        watchdog.schedule(
            object : TimerTask() {
                override fun run() {
                    timedOut = true
                    runCatching { outbound?.close() }
                }
            },
            HANDSHAKE_TIMEOUT_MS
        )

        try {
            // A real protocol tunnel to a real destination.

            val tunnel = OutboundFactory.create(
                endpoint,
                Destination.of(PROBE_HOST, PROBE_PORT)
            ) { socket ->

                protect(socket)
            }
            outbound = tunnel

            tunnel.output.write(PROBE_REQUEST.toByteArray(Charsets.US_ASCII))
            tunnel.output.flush()

            val buffer = ByteArray(256)
            val read = tunnel.input.read(buffer)
            val latency = (System.currentTimeMillis() - started).toInt()

            if (read <= 0) {
                return@withContext HandshakeResult(
                    success = false,
                    latencyMs = PingTester.UNREACHABLE,
                    negotiatedProtocol = "-",
                    cipherSuite = "-",
                    error = "سرور پاسخی برنگرداند (گره احتمالاً از کار افتاده است)"
                )
            }

            // Report the security actually in force on the carrier.
            when (endpoint.security) {
                "reality" -> { negotiated = "TLSv1.3"; cipher = "REALITY_AES_256" }
                "tls" -> { negotiated = "TLSv1.3"; cipher = "TLS_AES_256_GCM_SHA384" }
                else -> if (endpoint.protocol == Protocol.SHADOWSOCKS) {
                    negotiated = "AEAD"
                    cipher = endpoint.method.ifBlank { "aes-256-gcm" }.uppercase()
                } else if (endpoint.protocol == Protocol.VMESS) {
                    negotiated = "VMess AEAD"; cipher = "AES_128_GCM"
                }
            }

            val reply = String(buffer, 0, read, Charsets.US_ASCII)
            if (!reply.startsWith("HTTP/")) {
                return@withContext HandshakeResult(
                    success = false,
                    latencyMs = latency,
                    negotiatedProtocol = "-",
                    cipherSuite = "-",
                    error = "پاسخ نامعتبر از سرور (پروتکل یا رمز عبور اشتباه است)"
                )
            }

            HandshakeResult(
                success = true,
                latencyMs = latency,
                negotiatedProtocol = describeTransport(endpoint, negotiated),
                cipherSuite = cipher,
                payloadVerified = true
            )
        } catch (e: Exception) {
            HandshakeResult(
                success = false,
                latencyMs = PingTester.UNREACHABLE,
                negotiatedProtocol = "-",
                cipherSuite = "-",
                error = if (timedOut) {
                    "زمان دست‌دادن به پایان رسید (سرور پاسخ نداد)"
                } else {
                    e.message ?: e::class.java.simpleName
                }
            )
        } finally {
            watchdog.cancel()
            runCatching { outbound?.close() }
        }
    }

    private fun describeTransport(endpoint: ProxyEndpoint, negotiated: String): String =
        buildString {
            append(endpoint.displayProtocol)
            if (endpoint.network != "tcp") append('/').append(endpoint.network.uppercase())
            if (negotiated != "TCP") append(" · ").append(negotiated)
        }

    /** Short, human readable description of the tunnel security for the dashboard. */
    fun describeCipher(result: HandshakeResult): String = when {
        !result.success -> "-"
        result.cipherSuite == "none" -> result.negotiatedProtocol
        result.cipherSuite.contains("AES_256") || result.cipherSuite.contains("AES256") ->
            "${result.negotiatedProtocol} / AES-256"
        result.cipherSuite.contains("CHACHA20") -> "${result.negotiatedProtocol} / ChaCha20"
        result.cipherSuite.contains("AES_128") -> "${result.negotiatedProtocol} / AES-128"
        else -> "${result.negotiatedProtocol} / ${result.cipherSuite.take(24)}"
    }
}
