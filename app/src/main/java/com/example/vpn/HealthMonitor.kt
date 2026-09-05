package com.example.vpn

import com.example.vpn.stack.TcpStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watches a live tunnel and decides, from real evidence, whether it is healthy.
 *
 * A VPN can stay nominally "connected" long after it has stopped working: the
 * TUN device is up, the socket is open, but every new flow fails. Users see a
 * connected badge and a dead internet. This detects that state by watching the
 * ratio of failed to opened flows plus the byte counters, and reports a
 * degraded tunnel so the app can fail over instead of lying to the user.
 */
class HealthMonitor(
    private val scope: CoroutineScope,
    private val stack: () -> TcpStack?,
    private val onDegraded: (reason: String) -> Unit,
    private val onLog: (String) -> Unit
) {
    private var job: Job? = null

    /** Snapshot of the previous sample, to reason about deltas rather than totals. */
    private var lastOpened = 0L
    private var lastFailed = 0L
    private var lastBytesDown = 0L
    private var consecutiveBadSamples = 0

    fun start() {
        stop()
        lastOpened = 0
        lastFailed = 0
        lastBytesDown = 0
        consecutiveBadSamples = 0

        job = scope.launch {
            // Give the tunnel a moment to settle before judging it.
            delay(GRACE_PERIOD_MS)
            while (isActive) {
                delay(SAMPLE_INTERVAL_MS)
                val current = stack() ?: continue

                val opened = current.totalOpened
                val failed = current.totalFailed
                val bytesDown = current.bytesDown.get()

                val newFlows = opened - lastOpened
                val newFailures = failed - lastFailed
                val newBytes = bytesDown - lastBytesDown

                lastOpened = opened
                lastFailed = failed
                lastBytesDown = bytesDown

                // Only judge windows where the user actually tried to do something.
                if (newFlows < MIN_FLOWS_TO_JUDGE) {
                    consecutiveBadSamples = 0
                    continue
                }

                val failureRatio = newFailures.toFloat() / newFlows.toFloat()
                val starved = newBytes == 0L && newFlows >= MIN_FLOWS_TO_JUDGE

                if (failureRatio >= FAILURE_RATIO_THRESHOLD || starved) {
                    consecutiveBadSamples++
                    onLog(
                        "Health: %d/%d flows failed, %d bytes in (sample %d/%d)".format(
                            newFailures, newFlows, newBytes,
                            consecutiveBadSamples, BAD_SAMPLES_BEFORE_ACTION
                        )
                    )
                    if (consecutiveBadSamples >= BAD_SAMPLES_BEFORE_ACTION) {
                        consecutiveBadSamples = 0
                        val reason = if (starved) {
                            "تونل داده‌ای دریافت نمی‌کند"
                        } else {
                            "بیشتر اتصال‌ها ناموفق هستند"
                        }
                        onLog("Health: tunnel degraded — $reason")
                        onDegraded(reason)
                    }
                } else {
                    consecutiveBadSamples = 0
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val GRACE_PERIOD_MS = 12_000L
        const val SAMPLE_INTERVAL_MS = 6_000L

        /** Below this, the sample is too small to draw a conclusion from. */
        const val MIN_FLOWS_TO_JUDGE = 4

        /** Proportion of new flows that must fail for a sample to count as bad. */
        const val FAILURE_RATIO_THRESHOLD = 0.75f

        /** Require several consecutive bad samples so a blip is not a failover. */
        const val BAD_SAMPLES_BEFORE_ACTION = 3
    }
}
