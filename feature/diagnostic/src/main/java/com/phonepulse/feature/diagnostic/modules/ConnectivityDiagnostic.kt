package com.phonepulse.feature.diagnostic.modules

import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcManager
import android.telephony.TelephonyManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import javax.inject.Inject

class ConnectivityDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "connectivity"

    override suspend fun runAutomatic(context: Context): TestResult {
        val details = mutableMapOf<String, String>()
        val reasons = mutableListOf<String>()
        var score = 100
        var totalChecks = 0
        var passedChecks = 0

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val capabilities = network?.let { cm.getNetworkCapabilities(it) }
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false

            details["internet_available"] = "$hasInternet"
            details["transport_wifi"] = "$isWifi"
            details["transport_cellular"] = "$isCellular"

            totalChecks++
            if (hasInternet) {
                passedChecks++
                reasons.add(
                    "Internet: connected" + when {
                        isWifi -> " (Wi-Fi)"
                        isCellular -> " (Cellular)"
                        else -> ""
                    }
                )
            } else {
                reasons.add("Internet: not connected (-5)")
                score -= 5
            }
        } catch (e: Exception) {
            details["internet_error"] = e.message ?: "unknown"
            reasons.add("Internet check failed: ${e.message}")
        }

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            details["wifi_available"] = "${wifiManager != null}"

            totalChecks++
            if (wifiManager != null) {
                passedChecks++
                details["wifi_enabled"] = "${wifiManager.isWifiEnabled}"
                reasons.add("Wi-Fi hardware: present" + if (wifiManager.isWifiEnabled) " (enabled)" else " (disabled)")
            } else {
                score -= 10
                reasons.add("Wi-Fi hardware: not found (-10)")
            }
        } catch (e: Exception) {
            details["wifi_error"] = e.message ?: "unknown"
            reasons.add("Wi-Fi check error: ${e.message}")
        }

        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter

            totalChecks++
            if (btAdapter != null) {
                passedChecks++
                details["bluetooth_available"] = "true"
                details["bluetooth_le"] = "${btAdapter.isMultipleAdvertisementSupported}"
                reasons.add("Bluetooth: present (BLE: ${btAdapter.isMultipleAdvertisementSupported})")
            } else {
                details["bluetooth_available"] = "false"
                score -= 10
                reasons.add("Bluetooth: not found (-10)")
            }
        } catch (_: SecurityException) {
            details["bluetooth_available"] = "permission_denied"
            reasons.add("Bluetooth: permission denied (skipped)")
        } catch (e: Exception) {
            details["bluetooth_error"] = e.message ?: "unknown"
            reasons.add("Bluetooth check error: ${e.message}")
        }

        try {
            val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
            val nfcAdapter = nfcManager?.defaultAdapter

            totalChecks++
            details["nfc_available"] = "${nfcAdapter != null}"
            if (nfcAdapter != null) {
                passedChecks++
                details["nfc_enabled"] = "${nfcAdapter.isEnabled}"
                reasons.add("NFC: present" + if (nfcAdapter.isEnabled) " (enabled)" else " (disabled)")
            } else {
                score -= 3
                reasons.add("NFC: not available (-3)")
            }
        } catch (e: Exception) {
            details["nfc_error"] = e.message ?: "unknown"
            reasons.add("NFC check error: ${e.message}")
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            totalChecks++
            if (locationManager != null) {
                val gpsAvailable = try {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (_: Exception) {
                    false
                }

                val networkLocAvailable = try {
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (_: Exception) {
                    false
                }

                details["gps_provider"] = "$gpsAvailable"
                details["network_location"] = "$networkLocAvailable"

                if (gpsAvailable) {
                    passedChecks++
                    reasons.add("GPS: available")
                } else if (networkLocAvailable) {
                    passedChecks++
                    reasons.add("GPS hardware not found, but network location available")
                    score -= 5
                } else {
                    reasons.add("Location services: not available (-10)")
                    score -= 10
                }
            } else {
                reasons.add("LocationManager: not available (-10)")
                score -= 10
            }
        } catch (e: Exception) {
            details["gps_error"] = e.message ?: "unknown"
            reasons.add("GPS check error: ${e.message}")
        }

        try {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephony != null) {
                totalChecks++
                val simState = telephony.simState
                details["sim_state"] = when (simState) {
                    TelephonyManager.SIM_STATE_READY -> "ready"
                    TelephonyManager.SIM_STATE_ABSENT -> "absent"
                    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "pin_required"
                    TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "locked"
                    else -> "state_$simState"
                }

                details["network_operator"] = telephony.networkOperatorName ?: "none"
                details["phone_type"] = when (telephony.phoneType) {
                    TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                    TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                    TelephonyManager.PHONE_TYPE_SIP -> "SIP"
                    else -> "other"
                }

                if (simState == TelephonyManager.SIM_STATE_READY) {
                    passedChecks++
                    reasons.add("SIM: ready (${telephony.networkOperatorName ?: "operator unknown"})")
                } else if (simState == TelephonyManager.SIM_STATE_ABSENT) {
                    reasons.add("SIM: not inserted (normal if testing without SIM)")
                } else {
                    reasons.add("SIM state: ${details["sim_state"]}")
                }
            }
        } catch (_: SecurityException) {
            details["telephony_error"] = "permission_denied"
            reasons.add("Telephony: permission denied")
        } catch (e: Exception) {
            details["telephony_error"] = e.message ?: "unknown"
            reasons.add("Telephony check error")
        }

        details["checks_total"] = "$totalChecks"
        details["checks_passed"] = "$passedChecks"

        score = score.coerceIn(0, 100)

        if (score == 100 && passedChecks == 0 && totalChecks > 0) {
            score = 50
            reasons.add("No hardware checks passed (emulator?)")
        }

        val status = when {
            score >= 80 -> TestStatus.PASSED
            score >= 50 -> TestStatus.WARNING
            else -> TestStatus.FAILED
        }

        val summary = buildString {
            append("Passed: $passedChecks/$totalChecks checks")
            val features = mutableListOf<String>()
            if (details["wifi_available"] == "true") features.add("Wi-Fi")
            if (details["bluetooth_available"] == "true") features.add("BT")
            if (details["nfc_available"] == "true") features.add("NFC")
            if (details["gps_provider"] == "true") features.add("GPS")
            if (features.isNotEmpty()) append(" | ${features.joinToString(", ")}")
        }

        return TestResult(moduleName, score, status, details, summary)
    }
}
