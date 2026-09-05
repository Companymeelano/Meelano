package com.example.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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
        addressOf: (T) -> Pair<String, Int>?
    ): Map<String, Int> = coroutineScope {
        val results = LinkedHashMap<String, Int>()
        items.chunked(parallelism).forEach { chunk ->
            val measured = chunk.map { item ->
                async(Dispatchers.IO) {
                    val address = addressOf(item)
                    val latency =
                        if (address == null) UNREACHABLE
                        else ping(address.first, address.second, timeoutMs)
                    keyOf(item) to latency
                }
            }.awaitAll()
            results.putAll(measured)
        }
        results
    }
}
