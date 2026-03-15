package com.phonepulse.feature.diagnostic.modules

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticModule
import javax.inject.Inject

class DisplayDiagnostic @Inject constructor() : DiagnosticModule {
    override val moduleName = "display"

    override suspend fun runAutomatic(context: Context): TestResult {
        val details = mutableMapOf<String, String>()
        val reasons = mutableListOf<String>()
        var score = 100

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val display = if (Build.VERSION.SDK_INT >= 30) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        }

        if (display == null) {
            return TestResult(
                moduleName = moduleName,
                score = 50,
                status = TestStatus.WARNING,
                details = mapOf("error" to "cannot_access_display"),
                summary = "Cannot access display information"
            )
        }

        val mode = display.mode
        val metrics = context.resources.displayMetrics

        details["resolution"] = "${mode.physicalWidth}x${mode.physicalHeight}"
        details["refresh_rate_hz"] = "${"%.0f".format(mode.refreshRate)}"
        details["density_dpi"] = "${metrics.densityDpi}"

        val diagonal = Math.sqrt(
            Math.pow(mode.physicalWidth.toDouble() / metrics.xdpi, 2.0) +
                Math.pow(mode.physicalHeight.toDouble() / metrics.ydpi, 2.0)
        )
        details["size_inches"] = "${"%.1f".format(diagonal)}"

        val totalPixels = mode.physicalWidth.toLong() * mode.physicalHeight
        details["total_pixels"] = "$totalPixels"

        if (Build.VERSION.SDK_INT >= 26) {
            val hdr = display.hdrCapabilities
            val hdrSupported = hdr != null && hdr.supportedHdrTypes.isNotEmpty()
            details["hdr_supported"] = "$hdrSupported"
            if (hdrSupported) reasons.add("HDR supported")
        }

        when {
            totalPixels >= 3_686_400 -> {
                reasons.add("Excellent resolution: ${mode.physicalWidth}x${mode.physicalHeight}")
            }
            totalPixels >= 2_073_600 -> {
                reasons.add("Good resolution: ${mode.physicalWidth}x${mode.physicalHeight}")
            }
            totalPixels >= 921_600 -> {
                score -= 5
                reasons.add("-5: HD resolution (${mode.physicalWidth}x${mode.physicalHeight})")
            }
            else -> {
                score -= 15
                reasons.add("-15: Low resolution (${mode.physicalWidth}x${mode.physicalHeight})")
            }
        }

        when {
            mode.refreshRate >= 120 -> reasons.add("120Hz+ refresh rate")
            mode.refreshRate >= 90 -> reasons.add("90Hz refresh rate")
            mode.refreshRate >= 60 -> reasons.add("60Hz refresh rate (standard)")
            else -> {
                score -= 10
                reasons.add("-10: Below 60Hz refresh rate")
            }
        }

        score = score.coerceIn(0, 100)
        details["auto_score"] = "$score"
        details["note"] = "Interactive tests (pixels, touch, burn-in) will adjust this score"

        val status = when {
            score >= 80 -> TestStatus.PASSED
            score >= 50 -> TestStatus.WARNING
            else -> TestStatus.FAILED
        }

        val summary = buildString {
            append("${mode.physicalWidth}x${mode.physicalHeight}")
            append(" @ ${"%.0f".format(mode.refreshRate)}Hz")
            append(" | ${"%.1f".format(diagonal)}\"")
            append(" | Auto: $score/100")
            append(" (interactive tests pending)")
        }

        return TestResult(moduleName, score, status, details, summary)
    }
}
