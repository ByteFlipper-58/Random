package com.byteflipper.random.ui.components

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import com.byteflipper.random.data.settings.HapticsIntensity

interface HapticsManager {
    fun performPress(intensity: HapticsIntensity)

    /**
     * Short click for frequent events, such as a wheel sector passing the pointer.
     * Falls back to [performPress]; implementations may provide something lighter.
     */
    fun performTick(intensity: HapticsIntensity) = performPress(intensity)
}

class SystemHapticsManager(private val appContext: Context) : HapticsManager {

    // Ticks come in dozens per spin, so the service is resolved once instead of on every call.
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(durationMs: Int, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs.toLong(), amplitude)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs.toLong())
        }
    }

    override fun performPress(intensity: HapticsIntensity) {
        val (duration, amplitude) = when (intensity) {
            HapticsIntensity.Low -> 15 to 60
            HapticsIntensity.Medium -> 25 to 120
            HapticsIntensity.High -> 35 to 255
        }
        vibrate(duration, amplitude)
    }

    override fun performTick(intensity: HapticsIntensity) {
        val (duration, amplitude) = when (intensity) {
            HapticsIntensity.Low -> 5 to 40
            HapticsIntensity.Medium -> 8 to 70
            HapticsIntensity.High -> 12 to 110
        }
        vibrate(duration, amplitude)
    }
}

val LocalHapticsManager = staticCompositionLocalOf<HapticsManager?> { null }
