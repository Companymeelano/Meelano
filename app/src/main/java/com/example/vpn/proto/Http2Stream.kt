package com.example.vpn.proto

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A minimal HTTP/2 client, just enough to carry one long-lived gRPC stream.
 *
 * gRPC transports (`type=grpc`) are extremely common in real-world VLESS/VMess
 * configs, and they require genuine HTTP/2 framing — a plain TLS socket will be
 * rejected by the server. This implements exactly the subset needed:
 * connection preface, SETTINGS exchange, one HEADERS frame with a literal HPACK
 * encoding, and bidirectional DATA framing with WINDOW_UPDATE flow control.
 *
 * HPACK is used in its simplest legal form (literal header field without
 * indexing, never Huffman-coded), which every conforming server accepts.
 */
class Http2Stream(
    private val source: InputStream,
    private val sink: OutputStream,
    authority: String,
    path: String,
    userAgent: String
) {
    private val writeLock = Any()
    private val incoming = LinkedBlockingQueue<ByteArray>()

    @Volatile private var closed = false
    @Volatile private var failure: String? = null

    /** Bytes the peer has granted us; refreshed by WINDOW_UPDATE frames. */
    @Volatile private var sendWindow = 65_535L
    private var receivedSinceUpdate = 0L

    private val reader: Thread

    init {
        // ---- connection preface ----
        sink.write(PREFACE)
        // Empty SETTINGS frame (we accept the defaults).
        writeFrame(TYPE_SETTINGS, 0, 0, ByteArray(0))
        // Enlarge the connection-level receive window so downloads are not
        // throttled to the 64 KiB default.
        writeFrame(TYPE_WINDOW_UPDATE, 0, 0, encodeInt(WINDOW_INCREMENT))
        sink.flush()

        // ---- HEADERS: the gRPC call ----
        val headerBlock = buildHeaderBlock(authority, path, userAgent)
        writeFrame(TYPE_HEADERS, FLAG_END_HEADERS, STREAM_ID, headerBlock)
        writeFrame(TYPE_WINDOW_UPDATE, 0, STREAM_ID, encodeInt(WINDOW_INCREMENT))
        sink.flush()

        reader = Thread({ readLoop() }, "meelano-h2").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Payload stream. gRPC wraps every message in a 5-byte length prefix
     * (1 compression flag + 4 length bytes); we add and strip it transparently.
     */
    val input: InputStream = object : InputStream() {
        private var buffer = ByteArray(0)
        private var offset = 0

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) == 1) one[0].toInt() and 0xFF else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            while (offset >= buffer.size) {
                if (closed && incoming.isEmpty()) return -1
                val next = incoming.poll(70, TimeUnit.SECONDS)
                    ?: return if (closed) -1 else throw IOException(failure ?: "gRPC stream timed out")
                if (next.isEmpty()) return -1
                buffer = next
                offset = 0
            }
            val count = minOf(len, buffer.size - offset)
            buffer.copyInto(b, off, offset, offset + count)
            offset += count
            return count
        }

        override fun available(): Int = buffer.size - offset
    }

    val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (len <= 0) return
            failure?.let { throw IOException(it) }

            // gRPC length-prefixed message.
            val message = ByteArray(5 + len)
            message[0] = 0                                   // not compressed
            message[1] = ((len ushr 24) and 0xFF).toByte()
            message[2] = ((len ushr 16) and 0xFF).toByte()
            message[3] = ((len ushr 8) and 0xFF).toByte()
            message[4] = (len and 0xFF).toByte()
            b.copyInto(message, 5, off, off + len)

            synchronized(writeLock) {
                var written = 0
                while (written < message.size) {
                    val chunk = minOf(MAX_FRAME - 16, message.size - written)
                    writeFrame(
                        TYPE_DATA, 0, STREAM_ID,
                        message.copyOfRange(written, written + chunk)
                    )
                    written += chunk
                }
                sink.flush()
            }
        }

        override fun flush() = sink.flush()
    }

    // ---- frame plumbing ----------------------------------------------------

    private fun readLoop() {
        // gRPC messages can be split across DATA frames, so reassemble here.
        val pending = java.io.ByteArrayOutputStream()
        try {
            while (!closed) {
                val header = ByteArray(9)
                if (!readFully(header)) break
                val length = ((header[0].toInt() and 0xFF) shl 16) or
                    ((header[1].toInt() and 0xFF) shl 8) or
                    (header[2].toInt() and 0xFF)
                val type = header[3].toInt() and 0xFF
                val flags = header[4].toInt() and 0xFF
                val stream = ((header[5].toInt() and 0x7F) shl 24) or
                    ((header[6].toInt() and 0xFF) shl 16) or
                    ((header[7].toInt() and 0xFF) shl 8) or
                    (header[8].toInt() and 0xFF)

                val payload = ByteArray(length)
                if (length > 0 && !readFully(payload)) break

                when (type) {
                    TYPE_DATA -> {
                        pending.write(payload)
                        drainMessages(pending)

                        receivedSinceUpdate += length
                        if (receivedSinceUpdate > WINDOW_INCREMENT / 2) {
                            val delta = receivedSinceUpdate.toInt()
                            receivedSinceUpdate = 0
                            synchronized(writeLock) {
                                writeFrame(TYPE_WINDOW_UPDATE, 0, 0, encodeInt(delta))
                                writeFrame(TYPE_WINDOW_UPDATE, 0, STREAM_ID, encodeInt(delta))
                                sink.flush()
                            }
                        }
                        if (flags and FLAG_END_STREAM != 0) break
                    }

                    TYPE_SETTINGS -> if (flags and FLAG_ACK == 0) {
                        synchronized(writeLock) {
                            writeFrame(TYPE_SETTINGS, FLAG_ACK, 0, ByteArray(0))
                            sink.flush()
                        }
                    }

                    TYPE_PING -> if (flags and FLAG_ACK == 0) {
                        synchronized(writeLock) {
                            writeFrame(TYPE_PING, FLAG_ACK, 0, payload)
                            sink.flush()
                        }
                    }

                    TYPE_WINDOW_UPDATE -> {
                        val increment = ((payload[0].toLong() and 0x7F) shl 24) or
                            ((payload[1].toLong() and 0xFF) shl 16) or
                            ((payload[2].toLong() and 0xFF) shl 8) or
                            (payload[3].toLong() and 0xFF)
                        sendWindow += increment
                    }

                    TYPE_HEADERS -> {
                        // A trailers-only response means the call was rejected.
                        if (flags and FLAG_END_STREAM != 0 && stream == STREAM_ID) break
                    }

                    TYPE_RST_STREAM -> {
                        failure = "gRPC stream reset by server"
                        break
                    }

                    TYPE_GOAWAY -> {
                        failure = "gRPC connection closed by server (GOAWAY)"
                        break
                    }
                }
            }
        } catch (e: Exception) {
            failure = e.message ?: e::class.java.simpleName
        } finally {
            closed = true
            incoming.offer(ByteArray(0))   // unblock any waiting reader
        }
    }

    /** Extracts every complete 5-byte-prefixed gRPC message from the buffer. */
    private fun drainMessages(pending: java.io.ByteArrayOutputStream) {
        var data = pending.toByteArray()
        var consumed = 0
        while (data.size - consumed >= 5) {
            val size = ((data[consumed + 1].toInt() and 0xFF) shl 24) or
                ((data[consumed + 2].toInt() and 0xFF) shl 16) or
                ((data[consumed + 3].toInt() and 0xFF) shl 8) or
                (data[consumed + 4].toInt() and 0xFF)
            if (size < 0 || data.size - consumed - 5 < size) break
            incoming.offer(data.copyOfRange(consumed + 5, consumed + 5 + size))
            consumed += 5 + size
        }
        if (consumed > 0) {
            val rest = data.copyOfRange(consumed, data.size)
            pending.reset()
            pending.write(rest)
        }
    }

    private fun writeFrame(type: Int, flags: Int, streamId: Int, payload: ByteArray) {
        val header = ByteArray(9)
        header[0] = ((payload.size ushr 16) and 0xFF).toByte()
        header[1] = ((payload.size ushr 8) and 0xFF).toByte()
        header[2] = (payload.size and 0xFF).toByte()
        header[3] = type.toByte()
        header[4] = flags.toByte()
        header[5] = ((streamId ushr 24) and 0x7F).toByte()
        header[6] = ((streamId ushr 16) and 0xFF).toByte()
        header[7] = ((streamId ushr 8) and 0xFF).toByte()
        header[8] = (streamId and 0xFF).toByte()
        sink.write(header)
        if (payload.isNotEmpty()) sink.write(payload)
    }

    /**
     * Builds the HPACK header block for the gRPC POST.
     *
     * Uses indexed names where the static table has them, and literal-without-
     * indexing for the rest — legal, simple, and universally accepted.
     */
    private fun buildHeaderBlock(authority: String, path: String, userAgent: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(0x83)                                       // :method: POST (static index 3)
        out.write(0x87)                                       // :scheme: https (static index 7)
        literalIndexedName(out, 1, authority)                 // :authority
        literalIndexedName(out, 4, path)                      // :path
        literalNewName(out, "content-type", "application/grpc")
        literalIndexedName(out, 58, userAgent)                // user-agent
        literalNewName(out, "te", "trailers")
        literalNewName(out, "grpc-accept-encoding", "identity")
        return out.toByteArray()
    }

    /** Literal header field without indexing, name taken from the static table. */
    private fun literalIndexedName(out: java.io.ByteArrayOutputStream, index: Int, value: String) {
        out.write(index)                                      // 0000 pattern + index
        writeHpackString(out, value)
    }

    /** Literal header field without indexing, with a new (literal) name. */
    private fun literalNewName(out: java.io.ByteArrayOutputStream, name: String, value: String) {
        out.write(0x00)
        writeHpackString(out, name)
        writeHpackString(out, value)
    }

    /** HPACK string literal: 7-bit length prefix, no Huffman coding. */
    private fun writeHpackString(out: java.io.ByteArrayOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        if (bytes.size < 127) {
            out.write(bytes.size)
        } else {
            out.write(127)
            var remaining = bytes.size - 127
            while (remaining >= 128) {
                out.write((remaining and 0x7F) or 0x80)
                remaining = remaining ushr 7
            }
            out.write(remaining)
        }
        out.write(bytes)
    }

    private fun encodeInt(value: Int): ByteArray = byteArrayOf(
        ((value ushr 24) and 0x7F).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte()
    )

    private fun readFully(target: ByteArray): Boolean {
        var read = 0
        while (read < target.size) {
            val count = source.read(target, read, target.size - read)
            if (count < 0) return false
            read += count
        }
        return true
    }

    fun close() {
        closed = true
        incoming.offer(ByteArray(0))
    }

    private companion object {
        val PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".toByteArray(Charsets.US_ASCII)

        const val STREAM_ID = 1
        const val MAX_FRAME = 16_384
        const val WINDOW_INCREMENT = 8 * 1024 * 1024

        const val TYPE_DATA = 0x0
        const val TYPE_HEADERS = 0x1
        const val TYPE_RST_STREAM = 0x3
        const val TYPE_SETTINGS = 0x4
        const val TYPE_PING = 0x6
        const val TYPE_GOAWAY = 0x7
        const val TYPE_WINDOW_UPDATE = 0x8

        const val FLAG_ACK = 0x1
        const val FLAG_END_STREAM = 0x1
        const val FLAG_END_HEADERS = 0x4
    }
}
