package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import javax.inject.Inject

class ControlsDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "controls"

    override suspend fun runAutomatic(context: Context): TestResult {
        return try {
            val details = mutableMapOf<String, String>()
            var score = 100

            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val hasVibrator = vibrator.hasVibrator()
            details["vibrator_available"] = "$hasVibrator"
            if (hasVibrator) {
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                200,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                    details["vibrator_test"] = "triggered"
                } catch (_: Exception) {
                    details["vibrator_test"] = "error"
                    score -= 10
                }
            } else {
                score -= 15
            }

            val biometricManager = BiometricManager.from(context)
            val biometricStatus =
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            details["fingerprint_available"] = when (biometricStatus) {
                BiometricManager.BIOMETRIC_SUCCESS -> "ready"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "no_hardware"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "hw_unavailable"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "not_enrolled"
                else -> "unknown"
            }

            details["volume_keys"] = "pending_interactive_test"

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            val summary = buildString {
                append("Vibrator: ${if (hasVibrator) "OK" else "NO"}")
                append(" | Fingerprint: ${details["fingerprint_available"]}")
                append(" | Volume keys: pending")
            }

            TestResult(moduleName, score.coerceIn(0, 100), status, details, summary)
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
}
