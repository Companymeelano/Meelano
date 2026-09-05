package com.example.vpn.proto

import com.example.core.ProxyEndpoint
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.security.MessageDigest

/**
 * Genuine Trojan client.
 *
 * Trojan is deliberately minimal: after the TLS handshake the client sends
 * ```
 * hex(SHA224(password)) CRLF  CMD(1)  ADDR  CRLF
 * ```
 * and then raw payload. Everything is inside TLS, so on the wire it is
 * indistinguishable from ordinary HTTPS — which is what makes it survive DPI.
 */
class TrojanOutbound(
    endpoint: ProxyEndpoint,
    destination: Destination,
    protect: (Socket) -> Boolean
) : Outbound {

    private val carrier = Transport.open(endpoint, protect)

    init {
        val secret = endpoint.password.ifBlank { endpoint.userId }
        val token = sha224Hex(secret).toByteArray(Charsets.US_ASCII)
        val address = destination.encodeWithPortLast()

        val header = ByteArray(token.size + 2 + 1 + address.size + 2)
        var index = 0
        token.copyInto(header, index); index += token.size
        header[index++] = CR
        header[index++] = LF
        header[index++] = CMD_CONNECT
        address.copyInto(header, index); index += address.size
        header[index++] = CR
        header[index] = LF

        carrier.output.write(header)
        carrier.output.flush()
    }

    override val input: InputStream get() = carrier.input
    override val output: OutputStream get() = carrier.output
    override fun close() = carrier.close()

    private companion object {
        const val CR: Byte = 0x0D
        const val LF: Byte = 0x0A
        const val CMD_CONNECT: Byte = 1

        fun sha224Hex(password: String): String =
            MessageDigest.getInstance("SHA-224")
                .digest(password.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
