package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

/**
 * Real latency measurement: performs an actual TCP handshake against the
 * endpoint host/port and reports the round trip in milliseconds.
 *
 * There is no random number generator anywhere in this file — a node that is
 * unreachable reports [UNREACHABLE] and gets filtered out of the lists.
 */
object PingTester {

    const val UNREACHABLE = -1
    private const val DEFAULT_TIMEOUT_MS = 2500

    /** Measures a single endpoint. Returns latency in ms or [UNREACHABLE]. */
    suspend fun ping(
        host: String,
        port: Int,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        protect: (Socket) -> Boolean = { true }
    ): Int = withContext(Dispatchers.IO) {
        if (host.isBlank() || port !in 1..65535) return@withContext UNREACHABLE
        var socket: Socket? = null
        try {
            val elapsed = measureTimeMillis {
                socket = Socket().also {
                    protect(it)
                    it.tcpNoDelay = true
                    it.connect(InetSocketAddress(host, port), timeoutMs)
                }
            }
            elapsed.toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            UNREACHABLE
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Measures many endpoints concurrently (bounded by [parallelism]) and returns
     * a map of `key -> latency`. Used by "تست پینگ همه" and by the GitHub
     * subscription filter that keeps only the fastest live nodes.
     */
    suspend fun <T> pingAll(
        items: List<T>,
        parallelism: Int = 12,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
        keyOf: (T) -> String,
        addressOf: (T) -> Pair<String, Int>?,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Map<String, Int> = coroutineScope {
        val total = items.size
        val done = AtomicInteger()

        // A semaphore rather than chunked(): chunking made every batch wait for
        // its own slowest member, so a single node timing out stalled 31 idle
        // workers. With a permit pool each worker starts the moment one frees up.
        val gate = Semaphore(parallelism)

        // Every task must be individually fault-tolerant.
        //
        // coroutineScope + awaitAll propagates the first failure and cancels all
        // siblings, so one unexpected throw — a malformed host from a feed, a
        // resolver error, a socket the system refuses — aborted the entire sweep
        // partway through. That is what made stage 2 die suddenly mid-progress.
        val measured = items.map { item ->
            async(Dispatchers.IO) {
                val latency = runCatching {
                    gate.withPermit {
                        val address = addressOf(item)
                        if (address == null) UNREACHABLE
                        else ping(address.first, address.second, timeoutMs)
                    }
                }.getOrDefault(UNREACHABLE)
                // Report after every single probe. Without this the caller's
                // counter sat at zero for the whole sweep and the UI looked hung.
                //
                // Guarded too: the callback is supplied by the UI layer, and a
                // throw there would cancel the siblings just as surely as a
                // failing probe would.
                runCatching { onProgress(done.incrementAndGet(), total) }
                runCatching { keyOf(item) }.getOrDefault("unknown-${done.get()}") to latency
            }
        }.awaitAll()

        LinkedHashMap<String, Int>().apply { putAll(measured) }
    }
}
