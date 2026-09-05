package com.example.core

import com.example.vpn.proto.Destination
import com.example.vpn.proto.OutboundFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Socket
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

/**
 * Proves a node can genuinely carry traffic — not merely that its port is open.
 *
 * A TCP ping only shows that *something* is listening, which is why free lists
 * are full of nodes that connect instantly and then serve nothing. This runs the
 * node's real protocol handshake and asks it to proxy an actual HTTP request to
 * a third-party host, so only nodes that truly work survive.
 */
object NodeValidator {

    /** Small, globally available probe targets with tiny, predictable replies. */
    private val PROBES = listOf(
        Probe("www.gstatic.com", "/generate_204"),
        Probe("cp.cloudflare.com", "/"),
        Probe("www.google.com", "/generate_204")
    )

    private data class Probe(val host: String, val path: String)

    data class Result(
        val endpoint: ProxyEndpoint,
        val latencyMs: Int,
        val working: Boolean,
        val error: String? = null
    ) {
        val key: String get() = "${endpoint.host}:${endpoint.port}"
    }

    /**
     * Validates [endpoints] concurrently and returns only the ones that actually
     * relayed a response, sorted fastest first.
     *
     * @param probeTimeoutMs hard ceiling per node; anything slower is useless in
     *   practice, so it is treated as a failure.
     */
    suspend fun validateAll(
        endpoints: List<ProxyEndpoint>,
        parallelism: Int = 24,
        probeTimeoutMs: Long = 4_000,
        target: Int = Int.MAX_VALUE,
        protect: (Socket) -> Boolean = { true },
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): List<Result> = coroutineScope {
        val gate = Semaphore(parallelism)
        val completed = AtomicInteger()
        val passed = AtomicInteger()
        val total = endpoints.size

        val jobs = endpoints.map { endpoint ->
            async(Dispatchers.IO) {
                // Stop paying for probes once enough nodes have proven themselves.
                // Validating all 100 candidates when the user only sees 20 was the
                // main reason a refresh took the better part of a minute.
                if (passed.get() >= target) {
                    onProgress(completed.incrementAndGet(), total)
                    return@async null
                }

                val result = gate.withPermit {
                    if (passed.get() >= target) null
                    else validate(endpoint, probeTimeoutMs, protect)
                }
                if (result?.working == true) passed.incrementAndGet()
                onProgress(completed.incrementAndGet(), total)
                result
            }
        }

        jobs.awaitAll()
            .filterNotNull()
            .filter { it.working }
            .sortedBy { it.latencyMs }
    }

    /** Runs the full handshake-and-relay check against one node. */
    suspend fun validate(
        endpoint: ProxyEndpoint,
        timeoutMs: Long = 6_000,
        protect: (Socket) -> Boolean = { true }
    ): Result = withContext(Dispatchers.IO) {
        if (!OutboundFactory.supports(endpoint)) {
            return@withContext Result(
                endpoint = endpoint,
                latencyMs = PingTester.UNREACHABLE,
                working = false,
                error = OutboundFactory.unsupportedReason(endpoint)
            )
        }

        val probe = PROBES[Math.floorMod(endpoint.host.hashCode(), PROBES.size)]
        val started = System.currentTimeMillis()

        // As in TunnelEngine, a coroutine timeout cannot interrupt a blocking
        // socket read — the watchdog has to close the stream to unblock it.
        var tunnel: com.example.vpn.proto.Outbound? = null
        var timedOut = false
        val watchdog = Timer("meelano-probe-watchdog", true)
        watchdog.schedule(
            object : TimerTask() {
                override fun run() {
                    timedOut = true
                    runCatching { tunnel?.close() }
                }
            },
            timeoutMs
        )

        val outcome = run {
            try {
                val stream = OutboundFactory.create(
                    endpoint,
                    Destination.of(probe.host, 80),
                    protect
                )
                tunnel = stream

                val request = buildString {
                    append("HEAD ").append(probe.path).append(" HTTP/1.1\r\n")
                    append("Host: ").append(probe.host).append("\r\n")
                    append("User-Agent: ").append(com.example.vpn.proto.Transport.USER_AGENT)
                    append("\r\n")
                    append("Connection: close\r\n\r\n")
                }
                stream.output.write(request.toByteArray(Charsets.US_ASCII))
                stream.output.flush()

                val buffer = ByteArray(128)
                val read = stream.input.read(buffer)
                if (read <= 0) return@run "سرور پاسخی برنگرداند"

                val reply = String(buffer, 0, read, Charsets.US_ASCII)
                // A working relay always returns a well-formed status line.
                if (!reply.startsWith("HTTP/")) "پاسخ نامعتبر (رمز یا پروتکل اشتباه)" else null
            } catch (e: Exception) {
                if (timedOut) "زمان اتصال به پایان رسید" else e.message ?: e::class.java.simpleName
            } finally {
                watchdog.cancel()
                runCatching { tunnel?.close() }
            }
        }

        val elapsed = (System.currentTimeMillis() - started).toInt()
        when {
            timedOut ->
                Result(endpoint, PingTester.UNREACHABLE, false, "زمان اتصال به پایان رسید")

            outcome == null ->
                Result(endpoint, elapsed.coerceAtLeast(1), true)

            else ->
                Result(endpoint, PingTester.UNREACHABLE, false, outcome)
        }
    }
}
