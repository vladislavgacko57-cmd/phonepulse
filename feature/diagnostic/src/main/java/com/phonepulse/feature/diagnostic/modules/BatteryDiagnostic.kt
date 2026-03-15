package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import java.io.File
import javax.inject.Inject

class BatteryDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "battery"

    override suspend fun runAutomatic(context: Context): TestResult {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val details = mutableMapOf<String, String>()

            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            details["level_pct"] = "$level"

            val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            details["current_ua"] = "$currentNow"

            val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            details["charge_counter_uah"] = "$chargeCounter"

            val chargeFull = readSysFile("/sys/class/power_supply/battery/charge_full")
            val chargeFullDesign = readSysFile("/sys/class/power_supply/battery/charge_full_design")
            val cycleCount = readSysFile("/sys/class/power_supply/battery/cycle_count")

            var healthPct = 100
            if (chargeFull != null && chargeFullDesign != null && chargeFullDesign > 0) {
                healthPct = ((chargeFull.toDouble() / chargeFullDesign) * 100).toInt().coerceIn(0, 100)
                details["capacity_full_uah"] = "$chargeFull"
                details["capacity_design_uah"] = "$chargeFullDesign"
            }
            details["health_pct"] = "$healthPct"
            if (cycleCount != null) details["cycles"] = "$cycleCount"

            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { intent ->
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "unknown"

                details["temperature_c"] = "${temp / 10.0}"
                details["voltage_mv"] = "$voltage"
                details["technology"] = tech
                details["health_status"] = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                    BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
                    else -> "UNKNOWN"
                }
            }

            val score = when {
                healthPct >= 90 -> 100
                healthPct >= 80 -> 85
                healthPct >= 70 -> 70
                healthPct >= 60 -> 55
                healthPct >= 50 -> 40
                else -> 20
            }

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            TestResult(moduleName, score, status, details)
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

    private fun readSysFile(path: String): Long? = try {
        File(path).readText().trim().toLongOrNull()
    } catch (_: Exception) {
        null
    }
}
