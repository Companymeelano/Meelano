package com.example.vpn.proto

import com.example.core.ProxyEndpoint
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Builds the *carrier* beneath a proxy protocol: raw TCP, TLS, and WebSocket.
 *
 * VLESS/VMess/Trojan all ride on top of one of these. Getting the carrier right
 * (SNI, ALPN, the WebSocket upgrade) is what makes traffic survive DPI.
 */
object Transport {

    /** A current, ordinary-looking mobile Chrome UA. */
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    /** IPv4 dotted-quad or bracketless IPv6. */
    private val LITERAL_IP = Regex(
        """^((\d{1,3}\.){3}\d{1,3}|[0-9a-fA-F:]*:[0-9a-fA-F:]*)$"""
    )

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 60_000

    class Carrier(
        val socket: Socket,
        val input: InputStream,
        val output: OutputStream
    ) {
        fun close() = runCatching { socket.close() }.let { }
    }

    /**
     * Opens the transport to the proxy server itself (not the final destination).
     *
     * @param protect must be called on the raw socket *before* connecting so the
     *   VPN does not route its own uplink back into the tunnel.
     */
    fun open(endpoint: ProxyEndpoint, protect: (Socket) -> Boolean): Carrier {
        val raw = Socket()
        protect(raw)
        raw.tcpNoDelay = true
        raw.soTimeout = READ_TIMEOUT_MS
        raw.connect(InetSocketAddress(endpoint.host, endpoint.port), CONNECT_TIMEOUT_MS)

        val wantsTls = endpoint.security == "tls" ||
            endpoint.security == "reality" ||
            endpoint.protocol == com.example.core.Protocol.TROJAN

        var socket: Socket = raw
        if (wantsTls) {
            socket = upgradeToTls(raw, endpoint)
        }

        var input: InputStream = BufferedInputStream(socket.getInputStream(), 32 * 1024)
        var output: OutputStream = socket.getOutputStream()

        when (endpoint.network) {
            "ws" -> {
                performWebSocketUpgrade(endpoint, input, output)
                val framed = WebSocketStream(input, output)
                input = framed.input
                output = framed.output
            }

            // HTTPUpgrade: same handshake shape as WebSocket but the payload is
            // raw afterwards, with no frame headers at all.
            "httpupgrade" -> {
                performHttpUpgrade(endpoint, input, output)
            }

            "grpc", "gun" -> {
                val service = endpoint.serviceName.trim('/').ifBlank { "GunService" }
                val h2 = Http2Stream(
                    source = input,
                    sink = output,
                    authority = endpoint.effectiveHost,
                    path = "/$service/Tun",
                    userAgent = USER_AGENT
                )
                input = h2.input
                output = h2.output
            }
        }

        return Carrier(socket, input, output)
    }

    /**
     * True when the node dials a bare IP but presents someone else's SNI — the
     * classic domain-fronting shape.
     *
     * The certificate such a server returns is whatever the fronted CDN hands
     * out, so it can never chain-validate against the address we dialled. That
     * is expected, not an attack: VLESS/VMess/Trojan all carry their own
     * authentication and encryption inside the TLS tunnel, so the certificate is
     * only there to make the connection look ordinary on the wire. Insisting on
     * chain validation here is what produced "Trust anchor for certification
     * path not found" and killed every fronted node.
     */
    private fun isDomainFronted(endpoint: ProxyEndpoint): Boolean {
        val host = endpoint.host
        val sni = endpoint.effectiveSni
        if (sni.isBlank() || sni.equals(host, ignoreCase = true)) return false
        return LITERAL_IP.matches(host)
    }

    private fun upgradeToTls(raw: Socket, endpoint: ProxyEndpoint): SSLSocket {
        val sni = endpoint.effectiveSni
        val skipChainValidation = endpoint.allowInsecure ||
            endpoint.security == "reality" ||
            isDomainFronted(endpoint)

        val factory: SSLSocketFactory = if (skipChainValidation) {
            // Reality presents a borrowed certificate chain on purpose, so chain
            // validation against a public CA is meaningless for it.
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf(TrustAll), SecureRandom())
            context.socketFactory
        } else {
            SSLSocketFactory.getDefault() as SSLSocketFactory
        }

        val tls = factory.createSocket(raw, sni, endpoint.port, true) as SSLSocket
        tls.enabledProtocols = tls.supportedProtocols
            .filter { it == "TLSv1.3" || it == "TLSv1.2" }
            .toTypedArray()
            .ifEmpty { tls.supportedProtocols }

        // Send SNI explicitly; some stacks will not infer it from the socket host.
        runCatching {
            val params = tls.sslParameters
            params.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
            // Honour the config's ALPN exactly: CDN front-ends often reject a
            // handshake that offers h2 when the node expects http/1.1.
            val alpn = endpoint.alpn
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            params.applicationProtocols = when {
                alpn.isNotEmpty() -> alpn.toTypedArray()
                endpoint.network == "grpc" || endpoint.network == "gun" -> arrayOf("h2")
                endpoint.network == "ws" || endpoint.network == "httpupgrade" ->
                    arrayOf("http/1.1")
                else -> arrayOf("h2", "http/1.1")
            }
            tls.sslParameters = params
        }

        tls.soTimeout = READ_TIMEOUT_MS
        tls.startHandshake()
        return tls
    }

    /**
     * HTTPUpgrade transport: a plain `Upgrade: websocket` handshake after which
     * the connection carries raw bytes with no WebSocket framing. Cheaper than
     * `ws` and increasingly common behind CDNs.
     */
    private fun performHttpUpgrade(
        endpoint: ProxyEndpoint,
        input: InputStream,
        output: OutputStream
    ) {
        val host = endpoint.effectiveHost
        val path = endpoint.path.ifBlank { "/" }
        val request = buildString {
            append("GET ").append(path).append(" HTTP/1.1\r\n")
            append("Host: ").append(host).append("\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("User-Agent: ").append(USER_AGENT).append("\r\n")
            append("\r\n")
        }
        output.write(request.toByteArray(Charsets.US_ASCII))
        output.flush()

        val status = readHttpHeaders(input)
        if (!status.contains(" 101")) {
            throw java.io.IOException(
                "HTTPUpgrade rejected: ${status.lineSequence().firstOrNull().orEmpty()}"
            )
        }
    }

    private fun performWebSocketUpgrade(
        endpoint: ProxyEndpoint,
        input: InputStream,
        output: OutputStream
    ) {
        val key = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encodedKey = Base64.getEncoder().encodeToString(key)
        // The Host header must be the CDN-fronted name, which is often NOT the
        // address we dialled (that may be a bare IP).
        val host = endpoint.effectiveHost
        val path = endpoint.path.ifBlank { "/" }

        val request = buildString {
            append("GET ").append(path).append(" HTTP/1.1\r\n")
            append("Host: ").append(host).append("\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Key: ").append(encodedKey).append("\r\n")
            append("Sec-WebSocket-Version: 13\r\n")
            append("User-Agent: ").append(USER_AGENT).append("\r\n")
            append("\r\n")
        }
        output.write(request.toByteArray(Charsets.US_ASCII))
        output.flush()

        val status = readHttpHeaders(input)
        if (!status.contains(" 101")) {
            throw java.io.IOException("WebSocket upgrade rejected: ${status.lineSequence().first()}")
        }
    }

    private fun readHttpHeaders(input: InputStream): String {
        val builder = StringBuilder()
        var consecutive = 0
        while (consecutive < 2 && builder.length < 8192) {
            val b = input.read()
            if (b < 0) throw java.io.IOException("Server closed during WebSocket upgrade")
            val ch = b.toChar()
            builder.append(ch)
            when {
                ch == '\n' && builder.endsWith("\r\n") -> consecutive++
                ch == '\r' -> Unit
                else -> consecutive = 0
            }
        }
        return builder.toString()
    }

    private object TrustAll : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
