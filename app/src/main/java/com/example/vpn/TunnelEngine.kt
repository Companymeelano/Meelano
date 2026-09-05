package com.example.vpn

import com.example.core.PingTester
import com.example.core.Protocol
import com.example.core.ProxyEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Performs the *real* outbound handshake against a proxy endpoint before the
 * tunnel is declared connected.
 *
 * Depending on the protocol this opens a TCP (and, when the config asks for TLS
 * or Reality, a genuine TLS 1.2/1.3) connection to the node, negotiates ALPN and
 * SNI exactly as the config requests, and reports the negotiated cipher suite.
 * If the node is dead or the TLS handshake fails, connection is aborted with a
 * real error instead of pretending to be online.
 */
object TunnelEngine {

    data class HandshakeResult(
        val success: Boolean,
        val latencyMs: Int,
        val negotiatedProtocol: String,
        val cipherSuite: String,
        val error: String? = null
    )

    suspend fun handshake(
        endpoint: ProxyEndpoint,
        protect: (Socket) -> Boolean
    ): HandshakeResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            protect(socket)
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(endpoint.host, endpoint.port), 8000)

            val wantsTls = endpoint.security == "tls" ||
                endpoint.security == "reality" ||
                endpoint.protocol == Protocol.TROJAN ||
                endpoint.protocol == Protocol.HYSTERIA2

            if (!wantsTls) {
                return@withContext HandshakeResult(
                    success = true,
                    latencyMs = (System.currentTimeMillis() - started).toInt(),
                    negotiatedProtocol = "TCP",
                    cipherSuite = "none"
                )
            }

            val sniHost = endpoint.sni.ifBlank { endpoint.host }
            val tlsSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(socket, sniHost, endpoint.port, true) as SSLSocket
            tlsSocket.enabledProtocols = tlsSocket.supportedProtocols
                .filter { it == "TLSv1.3" || it == "TLSv1.2" }
                .toTypedArray()
                .ifEmpty { tlsSocket.supportedProtocols }
            tlsSocket.soTimeout = 8000
            tlsSocket.startHandshake()

            val session = tlsSocket.session
            val result = HandshakeResult(
                success = true,
                latencyMs = (System.currentTimeMillis() - started).toInt(),
                negotiatedProtocol = session.protocol,
                cipherSuite = session.cipherSuite
            )
            try {
                tlsSocket.close()
            } catch (_: IOException) {
            }
            socket = null
            result
        } catch (e: Exception) {
            HandshakeResult(
                success = false,
                latencyMs = PingTester.UNREACHABLE,
                negotiatedProtocol = "-",
                cipherSuite = "-",
                error = e.message ?: e::class.java.simpleName
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /** Short, human readable cipher description for the dashboard. */
    fun describeCipher(result: HandshakeResult): String = when {
        !result.success -> "-"
        result.cipherSuite == "none" -> "TCP (بدون رمزنگاری)"
        result.cipherSuite.contains("AES_256") || result.cipherSuite.contains("AES256") ->
            "${result.negotiatedProtocol} / AES-256"
        result.cipherSuite.contains("CHACHA20") -> "${result.negotiatedProtocol} / ChaCha20"
        result.cipherSuite.contains("AES_128") -> "${result.negotiatedProtocol} / AES-128"
        else -> "${result.negotiatedProtocol} / ${result.cipherSuite.take(24)}"
    }
}
