package com.phonepulse.core.common

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Programmatic sound effects for diagnostics.
 */
object SoundManager {

    suspend fun playSuccess() = withContext(Dispatchers.IO) {
        playTones(
            listOf(
                ToneSpec(523.25, 0.08),
                ToneSpec(659.25, 0.08),
                ToneSpec(783.99, 0.15)
            ),
            volume = 0.3f
        )
    }

    suspend fun playModuleComplete() = withContext(Dispatchers.IO) {
        playTones(
            listOf(ToneSpec(880.0, 0.05)),
            volume = 0.15f
        )
    }

    suspend fun playError() = withContext(Dispatchers.IO) {
        playTones(
            listOf(
                ToneSpec(440.0, 0.1),
                ToneSpec(330.0, 0.15)
            ),
            volume = 0.2f
        )
    }

    private data class ToneSpec(val frequencyHz: Double, val durationSec: Double)

    private fun playTones(tones: List<ToneSpec>, volume: Float) {
        val sampleRate = 44100
        val totalSamples = tones.sumOf { (sampleRate * it.durationSec).toInt() }
        val buffer = ShortArray(totalSamples)

        var offset = 0
        for (tone in tones) {
            val numSamples = (sampleRate * tone.durationSec).toInt()
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = when {
                    i < numSamples * 0.05 -> i.toDouble() / (numSamples * 0.05)
                    i > numSamples * 0.8 -> (numSamples - i).toDouble() / (numSamples * 0.2)
                    else -> 1.0
                }
                val sample = sin(2.0 * PI * tone.frequencyHz * t) * envelope * volume
                buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            offset += numSamples
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((tones.sumOf { it.durationSec } * 1000 + 100).toLong())
        } finally {
            runCatching { audioTrack.stop() }
            audioTrack.release()
        }
    }
}
