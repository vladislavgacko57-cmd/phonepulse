package com.phonepulse.core.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticManager {

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun tick(context: Context) {
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    fun click(context: Context) {
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, 100))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    fun success(context: Context) {
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= 26) {
            val timings = longArrayOf(0, 50, 80, 50, 80, 100)
            val amplitudes = intArrayOf(0, 80, 0, 120, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 50, 80, 50, 80, 100), -1)
        }
    }

    fun error(context: Context) {
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, 255))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    fun touchCell(context: Context) {
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, 30))
        }
    }
}
