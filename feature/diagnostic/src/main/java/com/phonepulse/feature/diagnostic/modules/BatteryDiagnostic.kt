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
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val details = mutableMapOf<String, String>()
        val reasons = mutableListOf<String>()

        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        details["level_pct"] = "$level"

        val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        details["current_ua"] = "$currentNow"

        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        details["charge_counter_uah"] = "$chargeCounter"

        val chargeFull = readSysFile("/sys/class/power_supply/battery/charge_full")
        val chargeFullDesign = readSysFile("/sys/class/power_supply/battery/charge_full_design")
        val cycleCount = readSysFile("/sys/class/power_supply/battery/cycle_count")

        var healthPct: Int? = null
        var healthSource = "unknown"

        if (chargeFull != null && chargeFullDesign != null && chargeFullDesign > 0) {
            healthPct = ((chargeFull.toDouble() / chargeFullDesign) * 100).toInt().coerceIn(0, 100)
            details["capacity_full_uah"] = "$chargeFull"
            details["capacity_design_uah"] = "$chargeFullDesign"
            details["capacity_full_mah"] = "${chargeFull / 1000}"
            details["capacity_design_mah"] = "${chargeFullDesign / 1000}"
            healthSource = "sysfs_capacity"
            reasons.add("Real capacity: ${chargeFull / 1000}mAh / ${chargeFullDesign / 1000}mAh = $healthPct%")
        } else {
            reasons.add("Cannot read battery capacity from sysfs (normal on some devices)")
        }

        if (cycleCount != null && cycleCount > 0) {
            details["cycles"] = "$cycleCount"
            reasons.add("Charge cycles: $cycleCount")
        } else {
            details["cycles"] = "N/A"
            reasons.add("Cycle count not available")
        }

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        var healthStatus = "UNKNOWN"

        batteryIntent?.let { intent ->
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "unknown"

            details["temperature_c"] = if (temp > 0) "${"%.1f".format(temp / 10.0)}" else "N/A"
            details["voltage_mv"] = if (voltage > 0) "$voltage" else "N/A"
            details["technology"] = tech

            healthStatus = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
                else -> "UNKNOWN"
            }
            details["health_status"] = healthStatus

            if (healthPct == null) {
                healthPct = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> 95
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> 60
                    BatteryManager.BATTERY_HEALTH_DEAD -> 10
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> 50
                    BatteryManager.BATTERY_HEALTH_COLD -> 70
                    else -> 85
                }
                healthSource = "system_health_api"
                reasons.add("Using system health API: $healthStatus")
            }
        }

        details["health_pct"] = "${healthPct ?: 85}"
        details["health_source"] = healthSource

        val hp = healthPct ?: 85
        var score = when {
            hp >= 95 -> 100
            hp >= 90 -> 95
            hp >= 85 -> 87
            hp >= 80 -> 80
            hp >= 75 -> 72
            hp >= 70 -> 65
            hp >= 60 -> 50
            hp >= 50 -> 35
            else -> 20
        }

        if (healthStatus == "OVERHEAT") {
            score -= 15
            reasons.add("-15: battery overheating")
        }
        if (healthStatus == "DEAD") {
            score -= 50
            reasons.add("-50: battery dead")
        }
        if (healthStatus == "OVER_VOLTAGE") {
            score -= 20
            reasons.add("-20: over voltage")
        }

        val tempC = details["temperature_c"]?.toDoubleOrNull()
        if (tempC != null && tempC > 40) {
            score -= 10
            reasons.add("-10: high temperature ${tempC}C")
        }

        val cycles = cycleCount?.toInt() ?: 0
        if (cycles in 1..200) {
            score += 5
            reasons.add("+5: low cycle count ($cycles)")
        } else if (cycles > 800) {
            score -= 10
            reasons.add("-10: high cycle count ($cycles)")
        }

        score = score.coerceIn(0, 100)
        reasons.add("Final score: $score")

        val status = when {
            score >= 80 -> TestStatus.PASSED
            score >= 50 -> TestStatus.WARNING
            else -> TestStatus.FAILED
        }

        val summary = buildString {
            append("Health: ${hp}%")
            if (healthSource == "sysfs_capacity") {
                append(" (real capacity measured)")
            } else {
                append(" (estimated from system API)")
            }
            if (cycles > 0) append(" | Cycles: $cycles")
            append(" | Status: $healthStatus")
        }

        return TestResult(moduleName, score, status, details, summary)
    }

    private fun readSysFile(path: String): Long? = try {
        File(path).readText().trim().toLongOrNull()
    } catch (_: Exception) {
        null
    }
}
