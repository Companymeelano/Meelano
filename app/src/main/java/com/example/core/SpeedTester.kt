package com.example.core

import com.example.vpn.proto.Destination
import com.example.vpn.proto.OutboundFactory
import com.example.vpn.proto.Transport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.util.Timer
import java.util.TimerTask

/**
 * Measures real throughput through a node by downloading a file over it.
 *
 * This is a genuine transfer, not an estimate derived from latency: it opens the
 * node's own protocol tunnel, requests a known-size payload from a public test
 * endpoint and reports the bytes that actually arrived per second. Latency and
 * bandwidth are only loosely related — a node can ping at 40 ms and still crawl
 * — so anything inferred from ping alone would be a guess presented as a fact.
 */
object SpeedTester {

    /**
     * Cloudflare's speed endpoint. It is globally anycast, allows an arbitrary
     * byte count and is plain HTTP on port 80, so no TLS cost is folded into the
     * measurement.
     */
    private const val TEST_HOST = "speed.cloudflare.com"
    private const val TEST_PORT = 80
    private const val TEST_BYTES = 2_000_000

    data class Report(
        val mbps: Double,
        val bytesTransferred: Long,
        val durationMs: Long,
        val error: String? = null
    ) {
        val success: Boolean get() = error == null && bytesTransferred > 0
    }

    /**
     * Runs a download test through [endpoint].
     *
     * @param budgetMs abort after this long and report the speed achieved so
     *   far, rather than failing — a slow node is still a measurable node.
     */
    suspend fun measure(
        endpoint: ProxyEndpoint,
        budgetMs: Long = 12_000,
        protect: (Socket) -> Boolean = { true },
        onProgress: (bytes: Long) -> Unit = {}
    ): Report = withContext(Dispatchers.IO) {
        if (!OutboundFactory.supports(endpoint)) {
            return@withContext Report(0.0, 0, 0, OutboundFactory.unsupportedReason(endpoint))
        }

        var tunnel: com.example.vpn.proto.Outbound? = null
        var expired = false

        // A blocking read cannot be cancelled by a coroutine timeout, so the
        // budget is enforced by closing the stream out from under it.
        val watchdog = Timer("meelano-speedtest", true)
        watchdog.schedule(
            object : TimerTask() {
                override fun run() {
                    expired = true
                    runCatching { tunnel?.close() }
                }
            },
            budgetMs
        )

        var received = 0L
        var startedTransfer = 0L

        try {
            val stream = OutboundFactory.create(
                endpoint,
                Destination.of(TEST_HOST, TEST_PORT),
                protect
            )
            tunnel = stream

            val request = buildString {
                append("GET /__down?bytes=").append(TEST_BYTES).append(" HTTP/1.1\r\n")
                append("Host: ").append(TEST_HOST).append("\r\n")
                append("User-Agent: ").append(Transport.USER_AGENT).append("\r\n")
                append("Accept: */*\r\n")
                append("Connection: close\r\n\r\n")
            }
            stream.output.write(request.toByteArray(Charsets.US_ASCII))
            stream.output.flush()

            val buffer = ByteArray(32 * 1024)

            // Skip the response headers so only payload bytes are timed, and
            // start the clock at the first body byte. Including the request
            // round-trip would blend latency into a bandwidth figure.
            var headerEnd = false
            var pending = ByteArray(0)
            while (!headerEnd) {
                val read = stream.input.read(buffer)
                if (read <= 0) return@withContext Report(0.0, 0, 0, "پاسخی دریافت نشد")
                pending += buffer.copyOf(read)
                val marker = indexOfHeaderEnd(pending)
                if (marker >= 0) {
                    headerEnd = true
                    val status = String(pending, 0, minOf(pending.size, 64), Charsets.US_ASCII)
                    if (!status.startsWith("HTTP/")) {
                        return@withContext Report(0.0, 0, 0, "پاسخ نامعتبر از سرور")
                    }
                    received += (pending.size - marker).toLong()
                    startedTransfer = System.currentTimeMillis()
                }
            }

            while (received < TEST_BYTES) {
                val read = stream.input.read(buffer)
                if (read <= 0) break
                received += read
                onProgress(received)
            }

            val elapsed = (System.currentTimeMillis() - startedTransfer).coerceAtLeast(1)
            Report(
                mbps = (received * 8.0) / (elapsed / 1000.0) / 1_000_000.0,
                bytesTransferred = received,
                durationMs = elapsed
            )
        } catch (e: Exception) {
            // Hitting the budget with data in hand is a valid measurement.
            if (expired && received > 0 && startedTransfer > 0) {
                val elapsed = (System.currentTimeMillis() - startedTransfer).coerceAtLeast(1)
                Report(
                    mbps = (received * 8.0) / (elapsed / 1000.0) / 1_000_000.0,
                    bytesTransferred = received,
                    durationMs = elapsed
                )
            } else {
                Report(0.0, received, 0, e.message ?: e::class.java.simpleName)
            }
        } finally {
            watchdog.cancel()
            runCatching { tunnel?.close() }
        }
    }

    /** Locates the blank line terminating the HTTP headers. */
    private fun indexOfHeaderEnd(data: ByteArray): Int {
        for (i in 0..data.size - 4) {
            if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() &&
                data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()
            ) {
                return i + 4
            }
        }
        return -1
    }
}
