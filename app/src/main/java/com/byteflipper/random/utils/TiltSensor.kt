package com.byteflipper.random.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.byteflipper.random.domain.ball.physics.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Reports which way is down and how hard the device is being moved, both in the coordinates the
 * screen is drawn in.
 *
 * Two streams, because the ball needs both: gravity decides where the liquid settles, while the
 * movement on top of it is what makes a shake look like a shake. Each prefers the sensor the
 * platform has already fused — `TYPE_GRAVITY` and `TYPE_LINEAR_ACCELERATION` — and falls back to the
 * raw accelerometer, splitting it into the two parts with a low pass. When both fall back to the
 * same sensor it is registered once.
 *
 * Sensor axes are fixed to the device, so readings are rotated by the display orientation on the way
 * out. Movement is reported in g, which is what the simulation's tuning is written in.
 */
class TiltSensor(
    private val context: Context,
    private val onGravity: (Vec3) -> Unit,
    private val onMotion: (Vec3) -> Unit = {}
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val motionSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isRegistered = false

    private var smoothedX = 0f
    private var smoothedY = -1f
    private var hasReading = false

    /** Running gravity estimate in m/s², kept so a raw accelerometer can be split into two streams. */
    private val gravityEstimate = FloatArray(3)
    private var hasGravityEstimate = false

    private var motionX = 0f
    private var motionY = 0f
    private var motionZ = 0f

    fun start() {
        if (isRegistered) return
        val gravity = gravitySensor
        val motion = motionSensor
        if (gravity == null && motion == null) return

        gravity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        // One sensor can feed both streams; registering it twice would only churn the rate.
        if (motion != null && motion != gravity) {
            sensorManager.registerListener(this, motion, SensorManager.SENSOR_DELAY_GAME)
        }
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        sensorManager.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (values.size < 3) return
        val sensor = event.sensor ?: return

        when (sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> emitMotion(values[0], values[1], values[2])

            Sensor.TYPE_GRAVITY -> {
                trackGravity(values)
                emitGravity(values)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // The accelerometer carries both parts at once: the slow half is gravity, and what
                // is left over is the movement.
                if (sensor == gravitySensor) trackGravity(values)
                if (sensor == gravitySensor) emitGravity(gravityEstimate)
                if (sensor == motionSensor && hasGravityEstimate) {
                    emitMotion(
                        values[0] - gravityEstimate[0],
                        values[1] - gravityEstimate[1],
                        values[2] - gravityEstimate[2]
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun trackGravity(values: FloatArray) {
        if (!hasGravityEstimate) {
            values.copyInto(gravityEstimate, endIndex = 3)
            hasGravityEstimate = true
            return
        }
        for (axis in 0 until 3) {
            gravityEstimate[axis] += (values[axis] - gravityEstimate[axis]) * GRAVITY_FOLLOW
        }
    }

    private fun emitGravity(values: FloatArray) {
        val length = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        if (length < 1e-3f) return

        // The sensors report proper acceleration, which points *away* from gravity.
        val deviceX = -values[0] / length
        val deviceY = -values[1] / length

        val (x, y) = rotateToDisplay(deviceX, deviceY)
        if (hasReading) {
            smoothedX += (x - smoothedX) * SMOOTHING
            smoothedY += (y - smoothedY) * SMOOTHING
        } else {
            smoothedX = x
            smoothedY = y
            hasReading = true
        }

        onGravity(Vec3(smoothedX, smoothedY, 0f))
    }

    /**
     * Passes on the movement in g, screen axes, with a light low pass and a deadzone.
     *
     * Sensor z already points out of the screen, which is the way the camera looks, so only x and y
     * need turning. The deadzone is what keeps a ball held in a resting hand from drifting.
     */
    private fun emitMotion(x: Float, y: Float, z: Float) {
        val (screenX, screenY) = rotateToDisplay(x, y)
        motionX += (screenX / SensorManager.GRAVITY_EARTH - motionX) * MOTION_SMOOTHING
        motionY += (screenY / SensorManager.GRAVITY_EARTH - motionY) * MOTION_SMOOTHING
        motionZ += (z / SensorManager.GRAVITY_EARTH - motionZ) * MOTION_SMOOTHING

        onMotion(
            Vec3(
                deadzone(motionX),
                deadzone(motionY),
                deadzone(motionZ)
            )
        )
    }

    private fun deadzone(value: Float): Float = if (abs(value) < MOTION_DEADZONE_G) 0f else value

    /** Turns device axes into screen axes for the current rotation. */
    private fun rotateToDisplay(x: Float, y: Float): Pair<Float, Float> =
        when (displayRotation()) {
            Surface.ROTATION_90 -> -y to x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> y to -x
            else -> x to y
        }

    private fun displayRotation(): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        return display?.rotation ?: Surface.ROTATION_0
    }

    private companion object {
        /** Gentle low-pass so a trembling hand does not slosh the liquid. */
        const val SMOOTHING = 0.18f

        /** How fast the gravity estimate follows a raw accelerometer; slow, so movement is left out. */
        const val GRAVITY_FOLLOW = 0.06f

        /** Lighter than the gravity pass: movement is meant to feel immediate. */
        const val MOTION_SMOOTHING = 0.45f

        /** Below this the reading is sensor noise, not a movement. */
        const val MOTION_DEADZONE_G = 0.02f
    }
}
