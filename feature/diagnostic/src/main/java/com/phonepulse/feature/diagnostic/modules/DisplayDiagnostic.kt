package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt

class DisplayDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "display"

    override suspend fun runAutomatic(context: Context): TestResult {
        return try {
            val details = mutableMapOf<String, String>()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val display = if (Build.VERSION.SDK_INT >= 30) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }

            display?.let { d ->
                val mode = d.mode
                details["resolution"] = "${mode.physicalWidth}x${mode.physicalHeight}"
                details["refresh_rate_hz"] = "${mode.refreshRate}"
                details["density_dpi"] = "${context.resources.displayMetrics.densityDpi}"
                details["size_inches"] = String.format(
                    "%.1f",
                    sqrt(
                        (mode.physicalWidth.toDouble() / context.resources.displayMetrics.xdpi).pow(2.0) +
                            (mode.physicalHeight.toDouble() / context.resources.displayMetrics.ydpi).pow(2.0)
                    )
                )
            }

            if (Build.VERSION.SDK_INT >= 26) {
                val hdr = display?.hdrCapabilities
                details["hdr_supported"] = "${hdr != null && hdr.supportedHdrTypes.isNotEmpty()}"
            }

            TestResult(
                moduleName = moduleName,
                score = 80,
                status = TestStatus.PASSED,
                details = details
            )
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
