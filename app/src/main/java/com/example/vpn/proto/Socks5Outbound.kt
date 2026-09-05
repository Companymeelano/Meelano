package com.example.vpn.proto

import com.example.core.ProxyEndpoint
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Genuine SOCKS5 client (RFC 1928) with optional username/password auth
 * (RFC 1929). Useful both for plain SOCKS nodes and as the fallback outbound.
 */
class Socks5Outbound(
    endpoint: ProxyEndpoint,
    destination: Destination,
    protect: (Socket) -> Boolean
) : Outbound {

    private val socket = Socket()
    override val input: InputStream
    override val output: OutputStream

    init {
        protect(socket)
        socket.tcpNoDelay = true
        socket.soTimeout = 60_000
        socket.connect(InetSocketAddress(endpoint.host, endpoint.port), 10_000)

        val source = socket.getInputStream().buffered(32 * 1024)
        val sink = socket.getOutputStream()

        val hasAuth = endpoint.userId.isNotBlank() || endpoint.password.isNotBlank()

        // ---- greeting ----
        if (hasAuth) {
            sink.write(byteArrayOf(0x05, 0x02, 0x00, 0x02))
        } else {
            sink.write(byteArrayOf(0x05, 0x01, 0x00))
        }
        sink.flush()

        val greeting = ByteArray(2)
        readFully(source, greeting)
        if (greeting[0] != 0x05.toByte()) throw IOException("Not a SOCKS5 server")

        if (greeting[1] == 0x02.toByte()) {
            val user = endpoint.userId.toByteArray(Charsets.UTF_8)
            val pass = endpoint.password.toByteArray(Charsets.UTF_8)
            val auth = ByteArray(3 + user.size + pass.size)
            var i = 0
            auth[i++] = 0x01
            auth[i++] = user.size.toByte()
            user.copyInto(auth, i); i += user.size
            auth[i++] = pass.size.toByte()
            pass.copyInto(auth, i)
            sink.write(auth)
            sink.flush()

            val authReply = ByteArray(2)
            readFully(source, authReply)
            if (authReply[1] != 0x00.toByte()) throw IOException("SOCKS5 authentication rejected")
        } else if (greeting[1] != 0x00.toByte()) {
            throw IOException("SOCKS5 server demands an unsupported auth method")
        }

        // ---- CONNECT ----
        val address = destination.encodeWithPortLast()
        val request = ByteArray(3 + address.size)
        request[0] = 0x05        // version
        request[1] = 0x01        // CONNECT
        request[2] = 0x00        // reserved
        address.copyInto(request, 3)
        sink.write(request)
        sink.flush()

        val reply = ByteArray(4)
        readFully(source, reply)
        if (reply[1] != 0x00.toByte()) {
            throw IOException("SOCKS5 CONNECT failed with code ${reply[1].toInt()}")
        }
        // Consume the bound address so the stream starts at real payload.
        when (reply[3].toInt()) {
            0x01 -> readFully(source, ByteArray(4 + 2))
            0x03 -> {
                val length = source.read()
                if (length < 0) throw IOException("SOCKS5 reply truncated")
                readFully(source, ByteArray(length + 2))
            }
            0x04 -> readFully(source, ByteArray(16 + 2))
            else -> throw IOException("Unknown SOCKS5 address type")
        }

        input = source
        output = sink
    }

    override fun close() {
        runCatching { socket.close() }
    }

    private companion object {
        fun readFully(source: InputStream, target: ByteArray) {
            var read = 0
            while (read < target.size) {
                val count = source.read(target, read, target.size - read)
                if (count < 0) throw IOException("SOCKS5 stream closed early")
                read += count
            }
        }
    }
}
