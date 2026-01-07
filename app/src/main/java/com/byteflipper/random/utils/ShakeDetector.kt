package com.byteflipper.random.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Utility class for detecting device shake gestures using accelerometer.
 *
 * @param context Application context
 * @param onShake Callback invoked when a shake gesture is detected
 * @param shakeThreshold Minimum acceleration required to register a shake (default 35.0f - requires firm shake)
 * @param shakeCooldownMs Minimum time between two shake events in milliseconds (default 1500ms)
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
    private val shakeThreshold: Float = 35.0f,
    private val shakeCooldownMs: Long = 1500L
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime: Long = 0L
    private var isRegistered = false

    /**
     * Start listening for shake events.
     * Call this in onResume() or when the screen becomes active.
     */
    fun start() {
        if (!isRegistered && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isRegistered = true
        }
    }

    /**
     * Stop listening for shake events.
     * Call this in onPause() or when the screen becomes inactive.
     */
    fun stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate acceleration magnitude minus gravity (approx 9.8 m/s^2)
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastShakeTime > shakeCooldownMs) {
                lastShakeTime = currentTime
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection
    }
}
