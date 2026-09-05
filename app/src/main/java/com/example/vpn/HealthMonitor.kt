package com.example.vpn

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
    /**
     * Health evidence for the current sample, or null if none is available yet.
     *
     * Taking a snapshot rather than a TcpStack lets the same detector serve both
     * engines. The Xray core owns its own TUN loop and exposes no flow table, so
     * binding this to TcpStack silently disabled degradation detection on what
     * is now the default connection path.
     */
    private val sample: () -> Sample?,
    private val onDegraded: (reason: String) -> Unit,
    private val onLog: (String) -> Unit
) {

    /**
     * One observation of tunnel liveness.
     *
     * @param opened cumulative flows opened, or 0 when the engine cannot report
     *   flows — [bytesDown] alone is then used to judge liveness.
     */
    data class Sample(
        val opened: Long,
        val failed: Long,
        val bytesDown: Long,
        /**
         * Cumulative bytes sent. Without a flow table this is what separates a
         * stalled tunnel from an idle user: traffic going out with nothing
         * coming back is a real fault, whereas silence in both directions just
         * means the phone is doing nothing.
         */
        val bytesUp: Long = 0L
    )

    private var job: Job? = null

    /** Snapshot of the previous sample, to reason about deltas rather than totals. */
    private var lastOpened = 0L
    private var lastFailed = 0L
    private var lastBytesDown = 0L
    private var lastBytesUp = 0L
    private var consecutiveBadSamples = 0

    fun start() {
        stop()
        lastOpened = 0
        lastFailed = 0
        lastBytesDown = 0
        lastBytesUp = 0
        consecutiveBadSamples = 0

        job = scope.launch {
            // Give the tunnel a moment to settle before judging it.
            delay(GRACE_PERIOD_MS)
            while (isActive) {
                delay(SAMPLE_INTERVAL_MS)
                val current = sample() ?: continue

                val opened = current.opened
                val failed = current.failed
                val bytesDown = current.bytesDown

                val newFlows = opened - lastOpened
                val newFailures = failed - lastFailed
                val newBytes = bytesDown - lastBytesDown
                val newBytesUp = current.bytesUp - lastBytesUp

                lastOpened = opened
                lastFailed = failed
                lastBytesDown = bytesDown
                lastBytesUp = current.bytesUp

                // Engines that expose no flow table (the Xray core) report
                // opened == 0. Judge those on throughput alone rather than
                // skipping every sample, which is what disabled detection on the
                // default path.
                val flowsKnown = opened > 0L

                if (flowsKnown) {
                    // Only judge windows where the user actually tried something.
                    if (newFlows < MIN_FLOWS_TO_JUDGE) {
                        consecutiveBadSamples = 0
                        continue
                    }
                } else if (newBytes > 0L || newBytesUp < MIN_UPLINK_TO_JUDGE) {
                    // Either data is arriving (tunnel alive), or the device sent
                    // essentially nothing, so there is nothing to conclude.
                    consecutiveBadSamples = 0
                    continue
                }

                val failureRatio =
                    if (newFlows > 0) newFailures.toFloat() / newFlows.toFloat() else 0f

                // Without a flow table, a silent window is the only evidence of
                // trouble available. It takes BAD_SAMPLES_BEFORE_ACTION of them
                // in a row to act, so an idle phone is not mistaken for a dead
                // tunnel.
                val starved = if (flowsKnown) {
                    newBytes == 0L && newFlows >= MIN_FLOWS_TO_JUDGE
                } else {
                    newBytes == 0L
                }

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
        /** Uplink in a window below which the user is considered idle. */
        const val MIN_UPLINK_TO_JUDGE = 4_096L

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
