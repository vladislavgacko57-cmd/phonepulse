package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class AudioDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "audio"

    override suspend fun runAutomatic(context: Context): TestResult = withContext(Dispatchers.IO) {
        val details = mutableMapOf<String, String>()
        val reasons = mutableListOf<String>()
        var score = 100

        val sampleRate = 44_100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        var micWorking = false
        var snrDb = 0.0

        if (bufferSize <= 0) {
            details["mic_error"] = "invalid_buffer_size"
            reasons.add("-30: Microphone buffer initialization failed")
            score -= 30
        } else {
            try {
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    details["mic_error"] = "not_initialized"
                    reasons.add("-30: Microphone could not initialize")
                    score -= 30
                } else {
                    recorder.startRecording()
                    val buffer = ShortArray(bufferSize / 2)

                    delay(300)
                    val readCount = recorder.read(buffer, 0, buffer.size)
                    recorder.stop()
                    recorder.release()

                    if (readCount > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            sumSquares += buffer[i].toDouble() * buffer[i].toDouble()
                        }
                        val rms = sqrt(sumSquares / readCount)
                        val maxAmplitude = buffer.take(readCount).maxOf { abs(it.toInt()) }

                        snrDb = if (rms > 1) 20 * log10(maxAmplitude.toDouble() / rms) else 0.0

                        details["mic_rms"] = "${"%.1f".format(rms)}"
                        details["mic_max_amplitude"] = "$maxAmplitude"
                        details["mic_snr_db"] = "${"%.1f".format(snrDb)}"

                        micWorking = rms > 5

                        if (!micWorking) {
                            reasons.add("-25: Microphone detected but no signal (RMS=${"%.1f".format(rms)})")
                            score -= 25
                        } else if (snrDb < 10) {
                            reasons.add("-10: Microphone works but poor quality (SNR=${"%.1f".format(snrDb)}dB)")
                            score -= 10
                        } else if (snrDb < 20) {
                            reasons.add("-5: Microphone quality below average (SNR=${"%.1f".format(snrDb)}dB)")
                            score -= 5
                        } else {
                            reasons.add("Microphone OK (SNR=${"%.1f".format(snrDb)}dB)")
                        }
                    } else {
                        reasons.add("-25: Microphone read returned no data")
                        score -= 25
                    }
                }
            } catch (e: SecurityException) {
                details["mic_error"] = "permission_denied"
                reasons.add("-20: Microphone permission denied (test skipped)")
                score -= 20
            } catch (e: Exception) {
                details["mic_error"] = e.message ?: "unknown"
                reasons.add("-20: Microphone error: ${e.message}")
                score -= 20
            }
        }

        details["mic_working"] = "$micWorking"

        var speakerWorking = false
        try {
            val atBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (atBufferSize > 0) {
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(atBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                val duration = 0.1
                val numSamples = (sampleRate * duration).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * 1000.0 * i / sampleRate
                    samples[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.3).toInt().toShort()
                }

                audioTrack.play()
                audioTrack.write(samples, 0, numSamples)
                delay(200)
                audioTrack.stop()
                audioTrack.release()
                speakerWorking = true
                reasons.add("Speaker OK (1kHz tone played)")
            } else {
                reasons.add("-15: Speaker buffer initialization failed")
                score -= 15
            }
        } catch (e: Exception) {
            details["speaker_error"] = e.message ?: "unknown"
            reasons.add("-15: Speaker error: ${e.message}")
            score -= 15
        }

        details["speaker_working"] = "$speakerWorking"
        if (!speakerWorking) {
            score -= 15
        }

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val hasEarpiece = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
        details["earpiece_available"] = "$hasEarpiece"
        if (hasEarpiece) {
            reasons.add("Earpiece detected")
        } else {
            reasons.add("No earpiece detected (normal for tablets)")
        }

        score = score.coerceIn(0, 100)

        val status = when {
            score >= 80 -> TestStatus.PASSED
            score >= 50 -> TestStatus.WARNING
            else -> TestStatus.FAILED
        }

        val summary = buildString {
            append("Speaker: ${if (speakerWorking) "OK" else "FAIL"}")
            append(" | Mic: ${if (micWorking) "OK" else "FAIL"}")
            if (micWorking) append(" (SNR: ${"%.0f".format(snrDb)}dB)")
            append(" | Earpiece: ${if (hasEarpiece) "YES" else "NO"}")
        }

        TestResult(moduleName, score, status, details, summary)
    }
}
