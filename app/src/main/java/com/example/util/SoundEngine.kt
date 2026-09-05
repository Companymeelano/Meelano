package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesises the app's interface sounds at runtime.
 *
 * Every cue is generated as PCM rather than shipped as an audio asset. That
 * keeps the APK unchanged, avoids licensing questions, and lets each sound be
 * tuned by editing numbers instead of re-recording. The palette is deliberately
 * restrained — short, soft, harmonically related tones — because interface
 * sounds that draw attention to themselves become irritating quickly.
 *
 * All tones are built from sine partials with an exponential decay envelope,
 * which is what makes them read as glass or chime rather than as a beep.
 */
object SoundEngine {

    private const val SAMPLE_RATE = 44_100

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Cheap cache: each cue is rendered once, then replayed from memory. */
    private val cache = HashMap<String, ShortArray>()

    @Volatile
    var muted: Boolean = false

    /** The interface cues, tuned to a pentatonic set so any pair sounds right. */
    enum class Cue(
        internal val partials: List<Partial>,
        internal val durationMs: Int,
        internal val decay: Float,
        internal val gain: Float
    ) {
        /** Light tick for ordinary taps. */
        TAP(listOf(Partial(1_320f, 1f), Partial(2_640f, 0.18f)), 70, 34f, 0.20f),

        /** Rising two-note chime as the tunnel comes up. */
        CONNECT(
            listOf(Partial(660f, 1f), Partial(990f, 0.55f), Partial(1_320f, 0.30f)),
            420, 7f, 0.32f
        ),

        /** Falling counterpart, played on disconnect. */
        DISCONNECT(
            listOf(Partial(494f, 1f), Partial(330f, 0.60f)),
            380, 8f, 0.28f
        ),

        /** Soft confirmation for a completed background task. */
        SUCCESS(
            listOf(Partial(880f, 1f), Partial(1_318f, 0.5f), Partial(1_760f, 0.25f)),
            300, 10f, 0.26f
        ),

        /** Muted, low double-thud for failures. Deliberately not harsh. */
        ERROR(
            listOf(Partial(220f, 1f), Partial(233f, 0.7f)),
            340, 9f, 0.30f
        ),

        /** Airy sweep used when a scan or refresh begins. */
        SCAN(
            listOf(Partial(587f, 1f), Partial(880f, 0.4f)),
            240, 12f, 0.18f
        )
    }

    internal data class Partial(val frequency: Float, val amplitude: Float)

    /** Plays [cue] unless sound is muted. Never blocks the caller. */
    fun play(cue: Cue) {
        if (muted) return
        scope.launch {
            runCatching {
                val samples = cache.getOrPut(cue.name) { render(cue) }
                writeAndPlay(samples)
            }
        }
    }

    /**
     * Renders one cue to 16-bit PCM.
     *
     * The envelope is `exp(-decay * t)` with a short linear fade-in. Without the
     * fade-in the waveform starts at full amplitude, and that discontinuity is
     * audible as a click on every play.
     */
    private fun render(cue: Cue): ShortArray {
        val count = SAMPLE_RATE * cue.durationMs / 1000
        val out = ShortArray(count)
        val attack = (SAMPLE_RATE * 0.004f).toInt().coerceAtLeast(1)
        val peak = cue.partials.sumOf { it.amplitude.toDouble() }.toFloat()

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            var value = 0f
            cue.partials.forEach { partial ->
                value += partial.amplitude * sin(2f * PI.toFloat() * partial.frequency * t)
            }
            value /= peak

            val envelope = exp(-cue.decay * t) *
                if (i < attack) i.toFloat() / attack else 1f

            out[i] = (value * envelope * cue.gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    private fun writeAndPlay(samples: ShortArray) {
        val bytes = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // Sonification, not media: this keeps interface cues off the
                    // music stream so they never duck or interrupt playback.
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bytes.coerceAtLeast(AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.setVolume(AudioManager.STREAM_SYSTEM.toFloat().coerceAtMost(1f))
        track.play()

        // MODE_STATIC keeps the buffer resident, so release only after playback
        // would have finished; releasing immediately truncates the sound.
        scope.launch {
            kotlinx.coroutines.delay(samples.size * 1000L / SAMPLE_RATE + 120L)
            runCatching {
                track.stop()
                track.release()
            }
        }
    }
}
