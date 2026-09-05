package com.example.vpn.proto

import com.example.core.ProxyEndpoint
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Genuine VLESS client (protocol version 0).
 *
 * Request header layout:
 * ```
 * 1 byte  version (0)
 * 16 bytes UUID
 * 1 byte  addon length (0 — no flow control addons)
 * 1 byte  command (1 = TCP)
 * 2 bytes port (big endian)
 * 1 byte  address type, then the address
 * ```
 * The response begins with `version` + `addon length` which must be consumed
 * before the caller sees the real payload.
 */
class VlessOutbound(
    endpoint: ProxyEndpoint,
    destination: Destination,
    protect: (Socket) -> Boolean
) : Outbound {

    private val carrier = Transport.open(endpoint, protect)
    private var responseHeaderConsumed = false

    init {
        val uuid = parseUuid(endpoint.userId)
        val address = destination.encodeWithPortFirst()
        val header = ByteBuffer.allocate(1 + 16 + 1 + 1 + address.size)
        header.put(0)                                   // version
        header.put(uuid)                                // 16-byte user id
        header.put(0)                                   // no addons
        header.put(CMD_TCP)
        header.put(address)                             // port + addr type + addr

        carrier.output.write(header.array())
        carrier.output.flush()
    }

    override val input: InputStream = object : InputStream() {
        override fun read(): Int {
            val single = ByteArray(1)
            return if (read(single, 0, 1) == 1) single[0].toInt() and 0xFF else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            consumeResponseHeader()
            return carrier.input.read(b, off, len)
        }
    }

    override val output: OutputStream = carrier.output

    /** VLESS replies with `version` + `addonLength` + addons before the payload. */
    private fun consumeResponseHeader() {
        if (responseHeaderConsumed) return
        responseHeaderConsumed = true

        val version = carrier.input.read()
        if (version < 0) throw IOException("VLESS server closed before responding")
        val addonLength = carrier.input.read()
        if (addonLength < 0) throw IOException("VLESS response truncated")
        if (addonLength > 0) {
            val skip = ByteArray(addonLength)
            var read = 0
            while (read < addonLength) {
                val count = carrier.input.read(skip, read, addonLength - read)
                if (count < 0) throw IOException("VLESS addon block truncated")
                read += count
            }
        }
    }

    override fun close() = carrier.close()

    private companion object {
        const val CMD_TCP: Byte = 1

        fun parseUuid(value: String): ByteArray {
            val uuid = runCatching { UUID.fromString(value.trim()) }.getOrElse {
                // Non-standard ids are hashed into a UUID exactly like Xray does.
                UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8))
            }
            return ByteBuffer.allocate(16)
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .array()
        }
    }
}
