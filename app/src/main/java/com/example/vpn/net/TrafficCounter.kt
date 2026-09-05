package com.example.vpn.net

import android.net.TrafficStats
import java.util.concurrent.atomic.AtomicLong

/**
 * Real traffic accounting.
 *
 * Every byte counted here was actually read from / written to the TUN file
 * descriptor (plus the kernel's per-uid counters as a cross-check). Speeds are
 * derived from elapsed wall clock time, never generated.
 */
class TrafficCounter(private val uid: Int) {

    private val tunRx = AtomicLong(0)
    private val tunTx = AtomicLong(0)

    private var lastSampleAt = 0L
    private var lastRx = 0L
    private var lastTx = 0L

    private val baselineRx = uidRxBytes()
    private val baselineTx = uidTxBytes()

    fun addRx(bytes: Int) {
        if (bytes > 0) tunRx.addAndGet(bytes.toLong())
    }

    fun addTx(bytes: Int) {
        if (bytes > 0) tunTx.addAndGet(bytes.toLong())
    }

    fun totalRxBytes(): Long = maxOf(tunRx.get(), uidRxBytes() - baselineRx)

    fun totalTxBytes(): Long = maxOf(tunTx.get(), uidTxBytes() - baselineTx)

    /** Returns `download to upload` speeds in megabits per second since the last sample. */
    fun sampleSpeedsMbps(): Pair<Float, Float> {
        val now = System.nanoTime()
        val rx = totalRxBytes()
        val tx = totalTxBytes()
        if (lastSampleAt == 0L) {
            lastSampleAt = now
            lastRx = rx
            lastTx = tx
            return 0f to 0f
        }
        val seconds = (now - lastSampleAt) / 1_000_000_000.0
        if (seconds <= 0.0) return 0f to 0f
        val down = ((rx - lastRx) * 8.0 / 1_000_000.0 / seconds).toFloat().coerceAtLeast(0f)
        val up = ((tx - lastTx) * 8.0 / 1_000_000.0 / seconds).toFloat().coerceAtLeast(0f)
        lastSampleAt = now
        lastRx = rx
        lastTx = tx
        return down to up
    }

    private fun uidRxBytes(): Long =
        TrafficStats.getUidRxBytes(uid).takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L

    private fun uidTxBytes(): Long =
        TrafficStats.getUidTxBytes(uid).takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L

    companion object {
        fun bytesToMb(bytes: Long): Float = (bytes / 1_048_576.0).toFloat()
    }
}
