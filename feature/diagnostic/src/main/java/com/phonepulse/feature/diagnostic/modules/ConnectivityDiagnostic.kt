package com.phonepulse.feature.diagnostic.modules

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.nfc.NfcManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.resume

class ConnectivityDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "connectivity"

    override suspend fun runAutomatic(context: Context): TestResult {
        return try {
            val details = mutableMapOf<String, String>()
            var score = 100

            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiEnabled = wifiManager?.isWifiEnabled ?: false
            details["wifi_available"] = "${wifiManager != null}"
            details["wifi_enabled"] = "$wifiEnabled"
            if (wifiManager == null) score -= 15

            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter
            details["bluetooth_available"] = "${bluetoothAdapter != null}"
            if (bluetoothAdapter != null) {
                details["bluetooth_name"] = bluetoothAdapter.name ?: "unknown"
                if (Build.VERSION.SDK_INT >= 33) {
                    details["bluetooth_le"] =
                        "${bluetoothAdapter.isMultipleAdvertisementSupported}"
                }
            } else {
                score -= 10
            }

            val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
            val nfcAdapter = nfcManager?.defaultAdapter
            details["nfc_available"] = "${nfcAdapter != null}"
            details["nfc_enabled"] = "${nfcAdapter?.isEnabled ?: false}"

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsAvailable =
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
            details["gps_available"] = "$gpsAvailable"
            if (!gpsAvailable) score -= 15

            if (gpsAvailable &&
                locationManager != null &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val ttff = withTimeout(5000L) {
                        suspendCancellableCoroutine<Long> { cont ->
                            val listener = object : LocationListener {
                                val startTime = System.currentTimeMillis()
                                override fun onLocationChanged(location: Location) {
                                    locationManager.removeUpdates(this)
                                    if (cont.isActive) {
                                        cont.resume(System.currentTimeMillis() - startTime)
                                    }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onStatusChanged(
                                    provider: String?,
                                    status: Int,
                                    extras: Bundle?
                                ) = Unit

                                override fun onProviderEnabled(provider: String) = Unit

                                override fun onProviderDisabled(provider: String) = Unit
                            }

                            if (Build.VERSION.SDK_INT >= 30) {
                                locationManager.getCurrentLocation(
                                    LocationManager.GPS_PROVIDER,
                                    null,
                                    ContextCompat.getMainExecutor(context)
                                ) { location ->
                                    if (location != null && cont.isActive) {
                                        cont.resume(0L)
                                    } else if (cont.isActive) {
                                        cont.resume(5000L)
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                locationManager.requestSingleUpdate(
                                    LocationManager.GPS_PROVIDER,
                                    listener,
                                    Looper.getMainLooper()
                                )
                                cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
                            }
                        }
                    }
                    details["gps_ttff_ms"] = "$ttff"
                } catch (_: Exception) {
                    details["gps_fix"] = "timeout_5s"
                    score -= 5
                }
            }

            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephony != null) {
                details["sim_state"] = when (telephony.simState) {
                    TelephonyManager.SIM_STATE_READY -> "ready"
                    TelephonyManager.SIM_STATE_ABSENT -> "absent"
                    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "pin_required"
                    else -> "other"
                }
                details["network_operator"] = telephony.networkOperatorName ?: "unknown"
                details["phone_type"] = when (telephony.phoneType) {
                    TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                    TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                    else -> "other"
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    details["data_network_type"] = when (telephony.dataNetworkType) {
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                        TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G HSPA+"
                        else -> "other"
                    }
                }
            }

            val status = when {
                score >= 80 -> TestStatus.PASSED
                score >= 50 -> TestStatus.WARNING
                else -> TestStatus.FAILED
            }

            TestResult(moduleName, score.coerceIn(0, 100), status, details)
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
