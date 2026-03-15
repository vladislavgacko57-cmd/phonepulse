package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import javax.inject.Inject

class CameraDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "camera"

    override suspend fun runAutomatic(context: Context): TestResult {
        return try {
            val details = mutableMapOf<String, String>()
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var score = 100

            val cameraIds = try {
                cameraManager.cameraIdList
            } catch (_: Exception) {
                emptyArray()
            }
            details["camera_count"] = "${cameraIds.size}"

            if (cameraIds.isEmpty()) {
                return TestResult(moduleName, 0, TestStatus.FAILED, mapOf("error" to "no_cameras"))
            }

            var rearFound = false
            var frontFound = false

            for (id in cameraIds) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    val label = when (facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> {
                            rearFound = true
                            "rear"
                        }

                        CameraCharacteristics.LENS_FACING_FRONT -> {
                            frontFound = true
                            "front"
                        }

                        else -> "camera_$id"
                    }

                    val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizes = configs?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                    val maxSize = sizes?.maxByOrNull { it.width * it.height }
                    if (maxSize != null) {
                        details["${label}_resolution"] = "${maxSize.width}x${maxSize.height}"
                        val megaPixels = (maxSize.width.toLong() * maxSize.height) / 1_000_000.0
                        details["${label}_megapixels"] = String.format("%.1f", megaPixels)
                    }

                    val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                    if (apertures != null && apertures.isNotEmpty()) {
                        details["${label}_aperture"] = "f/${apertures[0]}"
                    }

                    val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                    details["${label}_ois"] = "${oisModes != null && oisModes.contains(
                        CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )}"

                    val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    details["${label}_flash"] = "$hasFlash"

                    val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                    details["${label}_autofocus"] = "${afModes != null && afModes.size > 1}"
                } catch (e: Exception) {
                    details["camera_${id}_error"] = e.message ?: "unknown"
                    score -= 15
                }
            }

            if (!rearFound) score -= 40
            if (!frontFound) score -= 20
            details["rear_camera"] = "$rearFound"
            details["front_camera"] = "$frontFound"

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            val summary = buildString {
                append("Cameras: ${cameraIds.size}")
                if (rearFound) append(" | Rear: OK")
                if (frontFound) append(" | Front: OK")
                if (!rearFound) append(" | Rear: MISSING")
                if (!frontFound) append(" | Front: MISSING")
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
