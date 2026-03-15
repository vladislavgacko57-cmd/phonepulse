package com.phonepulse.feature.diagnostic

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.phonepulse.core.model.DeviceInfo
import java.io.File

object DeviceInfoCollector {

    fun collect(context: Context): DeviceInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val display = context.resources.displayMetrics
        val resolution = "${display.widthPixels}x${display.heightPixels}"

        val cpuModel = try {
            File("/proc/cpuinfo").readText().lines()
                .find { it.startsWith("Hardware") || it.startsWith("model name") }
                ?.substringAfter(":")
                ?.trim()
                ?: Build.HARDWARE
        } catch (_: Exception) {
            Build.HARDWARE
        }

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkLevel = Build.VERSION.SDK_INT,
            ramGb = memInfo.totalMem / 1_073_741_824.0,
            storageGb = stat.totalBytes / 1_073_741_824.0,
            screenResolution = resolution,
            cpuModel = cpuModel,
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    }
}
