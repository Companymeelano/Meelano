package com.example.desktop.core

import com.example.core.ProxyEndpoint
import com.example.vpn.proto.Destination
import com.example.vpn.proto.Outbound
import com.example.vpn.proto.OutboundFactory
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A local SOCKS5 and HTTP proxy that forwards everything through a MeeLano
 * outbound.
 *
 * This is the desktop counterpart of the phone's VpnService. Windows has no
 * equivalent of Android's VpnService permission model — capturing all system
 * traffic there needs a signed TAP/WinTun driver, which cannot be shipped in an
 * unsigned build. A local proxy is the honest alternative: it needs no driver
 * and no administrator rights, and every application that honours the Windows
 * proxy settings (all mainstream browsers, and anything using WinHTTP) is
 * carried through the tunnel for real.
 *
 * The limitation worth stating plainly: applications that ignore system proxy
 * settings will still go direct. This is a proxy, not a full-system VPN.
 *
 * Both protocols are served on one port by sniffing the first byte: 0x05 is a
 * SOCKS5 greeting, anything else is treated as an HTTP request line. That keeps
 * the user's configuration to a single host:port.
 */
class LocalProxyServer(
    private val port: Int = DEFAULT_PORT,
    private val onLog: (String) -> Unit = {}
) {

    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val workers = Executors.newCachedThreadPool { r ->
        Thread(r, "meelano-proxy").apply { isDaemon = true }
    }

    @Volatile
    private var endpoint: ProxyEndpoint? = null

    val bytesDown = AtomicLong()
    val bytesUp = AtomicLong()
    val activeFlows = AtomicLong()
    val totalFlows = AtomicLong()
    val failedFlows = AtomicLong()

    val isRunning: Boolean get() = running.get()
    val listenPort: Int get() = port

    /** Points every subsequent flow at [target]. Safe to call while running. */
    fun setEndpoint(target: ProxyEndpoint?) {
        endpoint = target
    }

    fun start(): Result<Unit> {
        if (running.get()) return Result.success(Unit)
        return runCatching {
            // Bound to loopback deliberately: a proxy listening on 0.0.0.0 is an
            // open relay that anyone on the same network can use.
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), port))
            serverSocket = socket
            running.set(true)

            workers.execute {
                onLog("پروکسی محلی روی 127.0.0.1:$port فعال شد")
                while (running.get()) {
                    val client = try {
                        socket.accept()
                    } catch (e: IOException) {
                        if (running.get()) onLog("خطا در پذیرش اتصال: ${e.message}")
                        break
                    }
                    workers.execute { handle(client) }
                }
            }
            Unit
        }.onFailure {
            running.set(false)
            onLog("راه‌اندازی پروکسی ناموفق بود: ${it.message}")
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
        activeFlows.set(0)
        onLog("پروکسی محلی متوقف شد")
    }

    fun resetCounters() {
        bytesDown.set(0)
        bytesUp.set(0)
        totalFlows.set(0)
        failedFlows.set(0)
    }

    // region connection handling

    private fun handle(client: Socket) {
        activeFlows.incrementAndGet()
        totalFlows.incrementAndGet()
        var outbound: Outbound? = null
        try {
            client.tcpNoDelay = true
            client.soTimeout = IDLE_TIMEOUT_MS

            val input = client.getInputStream().buffered()
            val output = client.getOutputStream()

            input.mark(1)
            val first = input.read()
            if (first < 0) return
            input.reset()

            val destination = if (first == 0x05) {
                negotiateSocks5(input, output)
            } else {
                negotiateHttp(input, output)
            } ?: return

            val target = endpoint
            if (target == null) {
                failedFlows.incrementAndGet()
                return
            }

            outbound = OutboundFactory.create(target, destination) { true }
            relay(input, output, outbound)
        } catch (e: Exception) {
            failedFlows.incrementAndGet()
        } finally {
            activeFlows.decrementAndGet()
            runCatching { outbound?.close() }
            runCatching { client.close() }
        }
    }

    /**
     * SOCKS5 greeting and CONNECT request (RFC 1928).
     *
     * Only "no authentication" is offered: the listener is on loopback, so a
     * password would guard nothing that the OS does not already guard.
     */
    private fun negotiateSocks5(input: InputStream, output: OutputStream): Destination? {
        val version = input.read()
        if (version != 0x05) return null
        val methodCount = input.read()
        repeat(methodCount.coerceAtLeast(0)) { input.read() }
        output.write(byteArrayOf(0x05, 0x00))
        output.flush()

        if (input.read() != 0x05) return null
        val command = input.read()
        input.read() // reserved
        val addressType = input.read()

        val host = when (addressType) {
            0x01 -> ByteArray(4).also { readFully(input, it) }
                .joinToString(".") { (it.toInt() and 0xFF).toString() }

            0x03 -> {
                val length = input.read()
                ByteArray(length).also { readFully(input, it) }.toString(Charsets.US_ASCII)
            }

            0x04 -> {
                val raw = ByteArray(16).also { readFully(input, it) }
                InetAddress.getByAddress(raw).hostAddress ?: return null
            }

            else -> return null
        }
        val port = ((input.read() and 0xFF) shl 8) or (input.read() and 0xFF)

        // Only CONNECT. BIND and UDP ASSOCIATE are refused explicitly rather
        // than left to time out.
        if (command != 0x01) {
            output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            output.flush()
            return null
        }

        output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        output.flush()
        return Destination.of(host, port)
    }

    /**
     * HTTP CONNECT for HTTPS, and absolute-URI GET/POST for plain HTTP.
     *
     * Windows applies its proxy setting to both, so both must work or half the
     * web breaks.
     */
    private fun negotiateHttp(input: InputStream, output: OutputStream): Destination? {
        val requestLine = readLine(input) ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 2) return null
        val method = parts[0]
        val target = parts[1]

        if (method.equals("CONNECT", ignoreCase = true)) {
            // Drain the remaining headers before switching to tunnel mode.
            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
            }
            val host = target.substringBeforeLast(':')
            val port = target.substringAfterLast(':').toIntOrNull() ?: 443
            output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
            output.flush()
            return Destination.of(host.removeSurrounding("[", "]"), port)
        }

        // Plain HTTP: the origin server is named in the absolute URI. The
        // request itself still has to reach the server, so it is replayed
        // through the tunnel below by the caller's relay loop — which is why
        // the head of the request is buffered and re-sent here.
        if (!target.startsWith("http://", ignoreCase = true)) return null
        val withoutScheme = target.removePrefix("http://").removePrefix("HTTP://")
        val authority = withoutScheme.substringBefore('/')
        val host = authority.substringBeforeLast(':')
        val port = authority.substringAfterLast(':', "").toIntOrNull() ?: 80

        pendingHead = buildString {
            val path = "/" + withoutScheme.substringAfter('/', "")
            append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
                // Proxy hop headers must not be forwarded to the origin.
                if (line.startsWith("Proxy-Connection", ignoreCase = true)) continue
                append(line).append("\r\n")
            }
            append("\r\n")
        }.toByteArray()

        return Destination.of(host, port)
    }

    /** Head of a rewritten plain-HTTP request, replayed once the tunnel opens. */
    private var pendingHead: ByteArray? = null

    private fun relay(clientIn: InputStream, clientOut: OutputStream, outbound: Outbound) {
        pendingHead?.let {
            outbound.output.write(it)
            outbound.output.flush()
            bytesUp.addAndGet(it.size.toLong())
            pendingHead = null
        }

        // Upstream on this thread, downstream on another: a single-threaded
        // copy would deadlock the moment both directions had data pending.
        val upstream = Thread {
            runCatching {
                val buffer = ByteArray(BUFFER)
                while (true) {
                    val read = clientIn.read(buffer)
                    if (read < 0) break
                    outbound.output.write(buffer, 0, read)
                    outbound.output.flush()
                    bytesUp.addAndGet(read.toLong())
                }
            }
            runCatching { outbound.close() }
        }
        upstream.isDaemon = true
        upstream.name = "meelano-up"
        upstream.start()

        runCatching {
            val buffer = ByteArray(BUFFER)
            while (true) {
                val read = outbound.input.read(buffer)
                if (read < 0) break
                clientOut.write(buffer, 0, read)
                clientOut.flush()
                bytesDown.addAndGet(read.toLong())
            }
        }
        upstream.join(1000)
    }

    // endregion

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) throw IOException("unexpected end of stream")
            offset += read
        }
    }

    /** Reads one CRLF-terminated line without over-reading into the body. */
    private fun readLine(input: InputStream): String? {
        val out = StringBuilder()
        while (true) {
            val c = input.read()
            if (c < 0) return if (out.isEmpty()) null else out.toString()
            if (c == '\n'.code) return out.toString().removeSuffix("\r")
            out.append(c.toChar())
            if (out.length > 8192) return out.toString()
        }
    }

    companion object {
        /** Chosen to avoid the ports common proxy tools already occupy. */
        const val DEFAULT_PORT = 10808
        private const val BUFFER = 32 * 1024
        private const val IDLE_TIMEOUT_MS = 120_000
    }
}
