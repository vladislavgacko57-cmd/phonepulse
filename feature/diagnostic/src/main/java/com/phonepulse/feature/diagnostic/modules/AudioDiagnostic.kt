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

    override suspend fun runAutomatic(context: Context): TestResult = try {
        withContext(Dispatchers.IO) {
            val details = mutableMapOf<String, String>()
            var score = 100

            val sampleRate = 44_100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            var micWorking = false
            var snrDb = 0.0

            if (bufferSize > 0) {
                try {
                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )

                    if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                        recorder.startRecording()
                        val buffer = ShortArray(bufferSize / 2)
                        delay(200)
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
                            snrDb = if (rms > 1) 20 * log10(maxAmplitude / rms) else 0.0

                            micWorking = rms > 10
                            details["mic_rms"] = String.format("%.1f", rms)
                            details["mic_max_amplitude"] = "$maxAmplitude"
                            details["mic_snr_db"] = String.format("%.1f", snrDb)
                        }
                    }
                } catch (e: SecurityException) {
                    details["mic_error"] = "permission_denied"
                    score -= 30
                } catch (e: Exception) {
                    details["mic_error"] = e.message ?: "unknown"
                    score -= 30
                }
            }

            details["mic_working"] = "$micWorking"
            if (!micWorking) score -= 40

            var speakerWorking = false
            try {
                val outBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (outBufferSize > 0) {
                    val track = AudioTrack.Builder()
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
                        .setBufferSizeInBytes(outBufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    val duration = 0.3
                    val numSamples = (sampleRate * duration).toInt()
                    val samples = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val angle = 2.0 * Math.PI * 1000.0 * i / sampleRate
                        samples[i] = (kotlin.math.sin(angle) * Short.MAX_VALUE * 0.5).toInt().toShort()
                    }

                    track.play()
                    track.write(samples, 0, numSamples)
                    delay(400)
                    track.stop()
                    track.release()
                    speakerWorking = true
                }
            } catch (e: Exception) {
                details["speaker_error"] = e.message ?: "unknown"
            }

            details["speaker_working"] = "$speakerWorking"
            if (!speakerWorking) score -= 30

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            details["earpiece_available"] = "${audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .any { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }}"

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            TestResult(moduleName, score.coerceIn(0, 100), status, details)
        }
    } catch (e: SecurityException) {
        TestResult(
            moduleName,
            0,
            TestStatus.SKIPPED,
            mapOf("error" to "permission_denied", "message" to (e.message ?: ""))
        )
    } catch (e: Exception) {
        TestResult(
            moduleName,
            0,
            TestStatus.FAILED,
            mapOf("error" to (e.message ?: "unknown_error"))
        )
    }
}
