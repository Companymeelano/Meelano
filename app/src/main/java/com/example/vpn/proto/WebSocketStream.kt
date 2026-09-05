package com.example.vpn.proto

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom

/**
 * Turns an already-upgraded WebSocket connection into a plain byte stream pair.
 *
 * Client frames are masked and sent as binary frames (RFC 6455 §5), which is the
 * exact framing v2ray-style `ws` transports expect; incoming frames are
 * unwrapped transparently, and control frames (ping/pong/close) are handled so
 * that long-lived tunnels stay alive behind CDNs.
 */
class WebSocketStream(
    private val source: InputStream,
    private val sink: OutputStream
) {
    private val random = SecureRandom()
    private val writeLock = Any()

    val input: InputStream = object : InputStream() {
        private var frame: ByteArray = EMPTY
        private var offset = 0

        override fun read(): Int {
            val single = ByteArray(1)
            return if (read(single, 0, 1) == 1) single[0].toInt() and 0xFF else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            while (offset >= frame.size) {
                frame = readFrame() ?: return -1
                offset = 0
            }
            val count = minOf(len, frame.size - offset)
            frame.copyInto(b, off, offset, offset + count)
            offset += count
            return count
        }

        override fun available(): Int = frame.size - offset
    }

    val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (len <= 0) return
            synchronized(writeLock) {
                writeFrame(OPCODE_BINARY, b, off, len)
                sink.flush()
            }
        }

        override fun flush() = sink.flush()
    }

    /** Reads one data frame, transparently answering pings and skipping control frames. */
    private fun readFrame(): ByteArray? {
        while (true) {
            val first = source.read()
            if (first < 0) return null
            val opcode = first and 0x0F

            val second = source.read()
            if (second < 0) return null
            val masked = second and 0x80 != 0
            var length = (second and 0x7F).toLong()

            when (length.toInt()) {
                126 -> length = ((readByte() shl 8) or readByte()).toLong()
                127 -> {
                    length = 0
                    repeat(8) { length = (length shl 8) or readByte().toLong() }
                }
            }
            if (length > MAX_FRAME) throw IOException("WebSocket frame too large: $length")

            val mask = if (masked) ByteArray(4) { readByte().toByte() } else null
            val payload = ByteArray(length.toInt())
            readFully(payload)
            if (mask != null) {
                for (i in payload.indices) payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
            }

            when (opcode) {
                OPCODE_BINARY, OPCODE_TEXT, OPCODE_CONTINUATION -> {
                    if (payload.isNotEmpty()) return payload
                }
                OPCODE_PING -> synchronized(writeLock) {
                    writeFrame(OPCODE_PONG, payload, 0, payload.size)
                    sink.flush()
                }
                OPCODE_CLOSE -> return null
                OPCODE_PONG -> Unit
            }
        }
    }

    private fun writeFrame(opcode: Int, data: ByteArray, off: Int, len: Int) {
        sink.write(0x80 or opcode)                     // FIN + opcode
        when {
            len < 126 -> sink.write(0x80 or len)       // MASK + length
            len <= 0xFFFF -> {
                sink.write(0x80 or 126)
                sink.write((len ushr 8) and 0xFF)
                sink.write(len and 0xFF)
            }
            else -> {
                sink.write(0x80 or 127)
                for (shift in 56 downTo 0 step 8) sink.write(((len.toLong() ushr shift) and 0xFF).toInt())
            }
        }
        val mask = ByteArray(4).also { random.nextBytes(it) }
        sink.write(mask)
        val masked = ByteArray(len)
        for (i in 0 until len) {
            masked[i] = (data[off + i].toInt() xor mask[i % 4].toInt()).toByte()
        }
        sink.write(masked)
    }

    private fun readByte(): Int {
        val value = source.read()
        if (value < 0) throw IOException("Unexpected end of WebSocket stream")
        return value
    }

    private fun readFully(target: ByteArray) {
        var read = 0
        while (read < target.size) {
            val count = source.read(target, read, target.size - read)
            if (count < 0) throw IOException("Unexpected end of WebSocket payload")
            read += count
        }
    }

    private companion object {
        val EMPTY = ByteArray(0)
        const val MAX_FRAME = 8L * 1024 * 1024
        const val OPCODE_CONTINUATION = 0x0
        const val OPCODE_TEXT = 0x1
        const val OPCODE_BINARY = 0x2
        const val OPCODE_CLOSE = 0x8
        const val OPCODE_PING = 0x9
        const val OPCODE_PONG = 0xA
    }
}
