package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.resume

class SensorsDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "sensors"

    private data class SensorCheck(
        val type: Int,
        val name: String,
        val required: Boolean = false
    )

    private val sensorsToCheck = listOf(
        SensorCheck(Sensor.TYPE_ACCELEROMETER, "accelerometer", required = true),
        SensorCheck(Sensor.TYPE_GYROSCOPE, "gyroscope", required = true),
        SensorCheck(Sensor.TYPE_MAGNETIC_FIELD, "magnetometer"),
        SensorCheck(Sensor.TYPE_PROXIMITY, "proximity"),
        SensorCheck(Sensor.TYPE_LIGHT, "light"),
        SensorCheck(Sensor.TYPE_PRESSURE, "barometer"),
        SensorCheck(Sensor.TYPE_STEP_COUNTER, "step_counter"),
        SensorCheck(Sensor.TYPE_GRAVITY, "gravity"),
        SensorCheck(Sensor.TYPE_ROTATION_VECTOR, "rotation_vector")
    )

    override suspend fun runAutomatic(context: Context): TestResult {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val details = mutableMapOf<String, String>()
            var score = 100

            val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            details["total_sensors"] = "${allSensors.size}"

            for (check in sensorsToCheck) {
                val sensor = sensorManager.getDefaultSensor(check.type)
                if (sensor == null) {
                    details[check.name] = "absent"
                    if (check.required) score -= 20
                    continue
                }

                val gotData = try {
                    withTimeout(2000L) {
                        waitForSensorData(sensorManager, sensor)
                    }
                } catch (_: Exception) {
                    false
                }

                details[check.name] = if (gotData) "working" else "no_data"
                details["${check.name}_vendor"] = sensor.vendor ?: ""
                details["${check.name}_resolution"] = "${sensor.resolution}"

                if (!gotData && check.required) score -= 15
                if (!gotData && !check.required) score -= 5
            }

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            val working = details.count { it.value == "working" }
            val total = sensorsToCheck.size
            val summary = "Sensors: $working/$total working | Total hardware sensors: ${allSensors.size}"

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

    private suspend fun waitForSensorData(sensorManager: SensorManager, sensor: Sensor): Boolean =
        suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && event.values.isNotEmpty()) {
                        sensorManager.unregisterListener(this)
                        if (cont.isActive) cont.resume(true)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
        }
}
