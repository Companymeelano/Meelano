package com.example.core

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Learns which nodes actually work for this user, on this network.
 *
 * Censorship is local and changes hour to hour: the node that works on one ISP
 * at 9pm may be dead on another at 9am. No static ranking can capture that, and
 * no remote model can either without being told the user's IP and browsing
 * patterns — which for the people this app is for is a real risk, not a
 * theoretical one.
 *
 * So the ranking is learned on the device from evidence the app already
 * collects: did the handshake succeed, how fast was it, and how long did the
 * tunnel survive. Nothing leaves the phone and no API key is involved.
 *
 * The selection rule is UCB1, the standard solution to the explore/exploit
 * trade-off. A node with a strong record is preferred, but one that has barely
 * been tried keeps a bonus that decays as evidence accumulates — so a newly
 * added node still gets a fair hearing instead of being starved by an
 * incumbent, and a node that starts failing is demoted quickly.
 */
class ConnectionAdvisor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("meelano_advisor", Context.MODE_PRIVATE)

    /** What we remember about one node. */
    data class Record(
        val attempts: Int = 0,
        val successes: Int = 0,
        val avgLatencyMs: Int = 0,
        /** Longest session this node has sustained, in seconds. */
        val bestHoldSeconds: Int = 0,
        val lastOutcomeAt: Long = 0L
    ) {
        val successRate: Double
            get() = if (attempts == 0) 0.0 else successes.toDouble() / attempts
    }

    /**
     * Records the result of a connection attempt.
     *
     * @param holdSeconds how long the tunnel stayed up. A connection that
     *   collapses after two seconds is not a success in any useful sense, so
     *   this is what separates "handshake completed" from "actually worked".
     */
    fun record(
        nodeKey: String,
        success: Boolean,
        latencyMs: Int = 0,
        holdSeconds: Int = 0
    ) {
        val current = load(nodeKey)
        val attempts = current.attempts + 1
        val successes = current.successes + if (success) 1 else 0

        // Running mean over successful attempts only; averaging in the latency
        // of a failure would reward nodes that fail fast.
        val avg = if (success && latencyMs > 0) {
            if (current.successes == 0) latencyMs
            else (current.avgLatencyMs * current.successes + latencyMs) / (current.successes + 1)
        } else {
            current.avgLatencyMs
        }

        save(
            nodeKey,
            Record(
                attempts = attempts,
                successes = successes,
                avgLatencyMs = avg,
                bestHoldSeconds = maxOf(current.bestHoldSeconds, holdSeconds),
                lastOutcomeAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Scores a node. Higher is better; used to order connection candidates.
     *
     * @param totalAttempts attempts across every node, which sets how strong the
     *   exploration bonus should be.
     */
    fun score(nodeKey: String, totalAttempts: Int): Double {
        val record = load(nodeKey)

        // Never tried: score above any node with a losing record, below any node
        // with a winning one, so unknowns are examined but not blindly trusted.
        if (record.attempts == 0) return UNTRIED_SCORE

        // Laplace smoothing keeps a single lucky success from reading as 100%.
        val rate = (record.successes + 1.0) / (record.attempts + 2.0)

        // Latency, normalised so 50ms scores ~1 and 1000ms scores ~0.
        val latencyScore = when {
            record.avgLatencyMs <= 0 -> 0.3
            else -> (1.0 - (record.avgLatencyMs / 1000.0)).coerceIn(0.0, 1.0)
        }

        // Stability matters more than speed: a fast tunnel that drops constantly
        // is worse than a slower one that holds.
        val stability = (record.bestHoldSeconds / 300.0).coerceIn(0.0, 1.0)

        // UCB1 exploration term.
        val exploration = if (totalAttempts > 0) {
            EXPLORATION * sqrt(ln(totalAttempts.toDouble() + 1) / record.attempts)
        } else {
            0.0
        }

        // Evidence goes stale: a result from last week says little about the
        // filtering in force right now, so old records drift back toward
        // "unknown" rather than being trusted indefinitely.
        val ageDays = (System.currentTimeMillis() - record.lastOutcomeAt) / 86_400_000.0
        val freshness = (1.0 - ageDays / STALE_AFTER_DAYS).coerceIn(0.25, 1.0)

        val merit = (rate * 0.55 + stability * 0.30 + latencyScore * 0.15) * freshness
        return merit + exploration
    }

    /** Orders [keys] best-first. */
    fun rank(keys: List<String>): List<String> {
        val total = keys.sumOf { load(it).attempts }
        return keys.sortedByDescending { score(it, total) }
    }

    /** Human-readable summary for the diagnostics screen. */
    fun describe(nodeKey: String): String {
        val record = load(nodeKey)
        if (record.attempts == 0) return "بدون سابقه"
        val percent = (record.successRate * 100).toInt()
        return "$percent٪ موفق از ${record.attempts} تلاش"
    }

    fun forget(nodeKey: String) {
        prefs.edit().remove(key(nodeKey)).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    private fun load(nodeKey: String): Record {
        val raw = prefs.getString(key(nodeKey), null) ?: return Record()
        val parts = raw.split('|')
        if (parts.size < 5) return Record()
        return Record(
            attempts = parts[0].toIntOrNull() ?: 0,
            successes = parts[1].toIntOrNull() ?: 0,
            avgLatencyMs = parts[2].toIntOrNull() ?: 0,
            bestHoldSeconds = parts[3].toIntOrNull() ?: 0,
            lastOutcomeAt = parts[4].toLongOrNull() ?: 0L
        )
    }

    private fun save(nodeKey: String, record: Record) {
        val encoded = listOf(
            record.attempts,
            record.successes,
            record.avgLatencyMs,
            record.bestHoldSeconds,
            record.lastOutcomeAt
        ).joinToString("|")
        prefs.edit().putString(key(nodeKey), encoded).apply()
    }

    private fun key(nodeKey: String) = "n_$nodeKey"

    private companion object {
        /** Sits between a losing record and a winning one. */
        const val UNTRIED_SCORE = 0.55

        /** Weight of the UCB1 exploration bonus. */
        const val EXPLORATION = 0.35

        /** Evidence older than this is heavily discounted. */
        const val STALE_AFTER_DAYS = 7.0
    }
}
