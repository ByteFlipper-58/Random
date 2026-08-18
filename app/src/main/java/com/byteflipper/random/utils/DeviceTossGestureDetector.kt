package com.byteflipper.random.utils

import com.byteflipper.random.domain.physics.Vec3
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/** Physical phone gesture that can throw dice without a button press. */
enum class DeviceTossGesture {
    /** Phone was near-flat, its top edge dipped, then snapped back. */
    TABLE_PITCH,

    /** Phone was upright and accelerated sharply against gravity. */
    VERTICAL_LIFT
}

/**
 * Recognises the two deliberate motions used to throw the 3D dice.
 *
 * Gravity supplies orientation and angular speed without accumulating gyro drift. Linear acceleration
 * supplies the upright gesture; integrating its upward component gives delta-velocity, so a longer or
 * faster lift really produces a stronger throw. Timestamps are sensor timestamps in nanoseconds.
 */
internal class DeviceTossGestureDetector(
    private val onToss: (DeviceTossGesture, Float) -> Unit
) {

    private var gravity = Vec3(0f, 0f, -1f)
    private var hasGravity = false
    private var lastGravityNanos = 0L
    private var lastPitch = 0f

    private var flatArmed = false
    private var pitching = false
    private var pitchDirection = 1f
    private var pitchStartedNanos = 0L
    private var peakPitch = 0f
    private var peakReturnSpeed = 0f
    private var peakGyroReturnSpeed = 0f
    private var returning = false
    private var returnStartedNanos = 0L
    private var recentGyroSpeed = 0f
    private var recentGyroNanos = 0L

    private var lastMotionNanos = 0L
    private var liftActive = false
    private var liftStartedNanos = 0L
    private var upwardImpulse = 0f
    private var peakUpwardAcceleration = 0f
    private var filteredMotion = Vec3.ZERO
    private var hasMotion = false
    private var motionNoise = BASE_MOTION_NOISE_G

    private var lastTossNanos = 0L

    fun reset() {
        gravity = Vec3(0f, 0f, -1f)
        hasGravity = false
        lastGravityNanos = 0L
        lastPitch = 0f
        resetPitch(keepArmed = false)
        resetLift()
        lastMotionNanos = 0L
        filteredMotion = Vec3.ZERO
        hasMotion = false
        motionNoise = BASE_MOTION_NOISE_G
        lastTossNanos = 0L
    }

    fun onGravity(sample: Vec3, timestampNanos: Long) {
        if (!sample.isFinite || sample.lengthSquared < Vec3.EPSILON) return
        val raw = sample.normalized(Vec3(0f, 0f, -1f))
        val deltaSeconds = secondsBetween(lastGravityNanos, timestampNanos)
        val next = if (!hasGravity || deltaSeconds <= 0f) {
            raw
        } else {
            val follow = (1f - exp(-deltaSeconds / GRAVITY_FILTER_SECONDS)).coerceIn(0.08f, 1f)
            gravity.lerp(raw, follow).normalized(raw)
        }
        val pitch = asin(next.y.coerceIn(-1f, 1f))
        gravity = next
        hasGravity = true

        val flat = abs(next.z) >= FLAT_ARM_Z && abs(next.x) <= FLAT_MAX_ROLL
        if (!pitching) {
            if (flat && abs(pitch) <= FLAT_ARM_PITCH) flatArmed = true
            if (flatArmed && abs(pitch) >= PITCH_ENTER_ANGLE) {
                pitching = true
                pitchDirection = if (pitch < 0f) -1f else 1f
                pitchStartedNanos = timestampNanos
                peakPitch = abs(pitch)
                peakReturnSpeed = 0f
                peakGyroReturnSpeed = 0f
                returning = false
                returnStartedNanos = 0L
            }
        } else {
            val elapsed = secondsBetween(pitchStartedNanos, timestampNanos)
            if (elapsed > PITCH_TIMEOUT_SECONDS) {
                resetPitch(keepArmed = flat)
            } else {
                val signedPitch = pitch * pitchDirection
                val previousSignedPitch = lastPitch * pitchDirection
                val reachedDeeperPoint = signedPitch > peakPitch + RETURN_DIRECTION_EPSILON
                if (reachedDeeperPoint) {
                    // A tiny hand wobble while the phone is still being tipped must not start the
                    // return timer. A new deepest point always begins a fresh possible snap.
                    peakPitch = signedPitch
                    peakReturnSpeed = 0f
                    peakGyroReturnSpeed = 0f
                    returning = false
                    returnStartedNanos = 0L
                }
                if (deltaSeconds > 0f && signedPitch < previousSignedPitch) {
                    if (previousSignedPitch - signedPitch >= RETURN_DIRECTION_EPSILON) {
                        if (!returning) {
                            returning = true
                            returnStartedNanos = timestampNanos
                            if (timestampNanos - recentGyroNanos <= RECENT_GYRO_WINDOW_NANOS) {
                                peakGyroReturnSpeed = recentGyroSpeed
                            }
                        }
                    }
                    peakReturnSpeed = max(
                        peakReturnSpeed,
                        (previousSignedPitch - signedPitch) / deltaSeconds
                    )
                }

                val returned = abs(pitch) <= PITCH_RETURN_ANGLE && abs(next.z) >= PITCH_RETURN_Z
                if (returned) {
                    val measuredReturnSpeed = max(peakReturnSpeed, peakGyroReturnSpeed)
                    val returnSeconds = secondsBetween(returnStartedNanos, timestampNanos)
                    val sharpReturn = returning && returnSeconds in
                        MIN_SHARP_RETURN_SECONDS..MAX_SHARP_RETURN_SECONDS
                    if (peakPitch >= PITCH_MIN_ANGLE &&
                        sharpReturn && measuredReturnSpeed >= PITCH_MIN_RETURN_SPEED
                    ) {
                        val speed = score(
                            measuredReturnSpeed,
                            PITCH_MIN_RETURN_SPEED,
                            PITCH_FULL_RETURN_SPEED
                        )
                        val depth = score(peakPitch, PITCH_MIN_ANGLE, PITCH_FULL_ANGLE)
                        emit(DeviceTossGesture.TABLE_PITCH, speed * 0.8f + depth * 0.2f, timestampNanos)
                    }
                    resetPitch(keepArmed = true)
                }
            }
        }

        lastPitch = pitch
        lastGravityNanos = timestampNanos
    }

    /** Gyroscope supplies the fast edge that a filtered gravity vector can soften. Values are rad/s. */
    fun onGyroscope(rotation: Vec3, timestampNanos: Long) {
        if (!rotation.isFinite || timestampNanos <= 0L || !pitching) return
        recentGyroSpeed = abs(rotation.x)
        recentGyroNanos = timestampNanos
        if (returning) peakGyroReturnSpeed = max(peakGyroReturnSpeed, recentGyroSpeed)
    }

    fun onMotion(acceleration: Vec3, timestampNanos: Long) {
        if (!acceleration.isFinite) return
        val deltaSeconds = secondsBetween(lastMotionNanos, timestampNanos)
            .takeIf { it > 0f } ?: DEFAULT_SENSOR_STEP_SECONDS
        lastMotionNanos = timestampNanos

        val motionFollow = if (!hasMotion) {
            1f
        } else {
            (1f - exp(-deltaSeconds / MOTION_FILTER_SECONDS)).coerceIn(0.15f, 1f)
        }
        filteredMotion = filteredMotion.lerp(acceleration, motionFollow)
        hasMotion = true

        val horizontalGravity = sqrt(gravity.x * gravity.x + gravity.y * gravity.y)
        val upright = hasGravity && abs(gravity.z) <= VERTICAL_MAX_Z &&
            horizontalGravity >= VERTICAL_MIN_GRAVITY
        if (!upright) {
            resetLift()
            filteredMotion = Vec3.ZERO
            hasMotion = false
            return
        }

        // Up is exactly opposite the current gravity vector, in any display rotation.
        val upward = filteredMotion dot -gravity
        val rawUpward = acceleration dot -gravity
        if (!liftActive) {
            val adaptiveStart = max(LIFT_START_G, motionNoise * MOTION_NOISE_MULTIPLIER)
            if (upward < adaptiveStart) {
                val quietMagnitude = filteredMotion.length.coerceAtMost(MAX_TRACKED_NOISE_G)
                motionNoise += (quietMagnitude - motionNoise) * MOTION_NOISE_FOLLOW
                return
            }
            liftActive = true
            liftStartedNanos = timestampNanos
            upwardImpulse = upward * deltaSeconds
            peakUpwardAcceleration = upward
            return
        }

        if (upward > 0f) upwardImpulse += upward * deltaSeconds
        peakUpwardAcceleration = max(peakUpwardAcceleration, upward)
        val elapsed = secondsBetween(liftStartedNanos, timestampNanos)
        val ended = rawUpward <= LIFT_END_G || elapsed >= LIFT_MAX_SECONDS
        if (!ended) return

        if (upwardImpulse >= LIFT_MIN_IMPULSE && peakUpwardAcceleration >= LIFT_MIN_PEAK_G) {
            val velocity = score(upwardImpulse, LIFT_MIN_IMPULSE, LIFT_FULL_IMPULSE)
            val peak = score(peakUpwardAcceleration, LIFT_MIN_PEAK_G, LIFT_FULL_PEAK_G)
            emit(DeviceTossGesture.VERTICAL_LIFT, velocity * 0.72f + peak * 0.28f, timestampNanos)
        }
        resetLift()
    }

    private fun emit(gesture: DeviceTossGesture, strength: Float, timestampNanos: Long) {
        if (lastTossNanos != 0L &&
            timestampNanos - lastTossNanos < TOSS_COOLDOWN_NANOS
        ) {
            return
        }
        lastTossNanos = timestampNanos
        onToss(gesture, strength.coerceIn(MIN_OUTPUT_STRENGTH, 1f))
    }

    private fun resetPitch(keepArmed: Boolean) {
        flatArmed = keepArmed
        pitching = false
        pitchDirection = 1f
        pitchStartedNanos = 0L
        peakPitch = 0f
        peakReturnSpeed = 0f
        peakGyroReturnSpeed = 0f
        returning = false
        returnStartedNanos = 0L
        recentGyroSpeed = 0f
        recentGyroNanos = 0L
    }

    private fun resetLift() {
        liftActive = false
        liftStartedNanos = 0L
        upwardImpulse = 0f
        peakUpwardAcceleration = 0f
    }

    private fun secondsBetween(earlier: Long, later: Long): Float {
        if (earlier <= 0L || later <= earlier) return 0f
        return (later - earlier) / NANOS_PER_SECOND
    }

    private fun score(value: Float, minimum: Float, full: Float): Float =
        ((value - minimum) / (full - minimum)).coerceIn(0f, 1f)

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000f
        const val DEFAULT_SENSOR_STEP_SECONDS = 1f / 50f
        const val TOSS_COOLDOWN_NANOS = 650_000_000L
        const val MIN_OUTPUT_STRENGTH = 0.12f

        const val GRAVITY_FILTER_SECONDS = 0.035f
        const val RETURN_DIRECTION_EPSILON = 0.006f
        const val RECENT_GYRO_WINDOW_NANOS = 70_000_000L
        const val MOTION_FILTER_SECONDS = 0.018f
        const val BASE_MOTION_NOISE_G = 0.025f
        const val MOTION_NOISE_FOLLOW = 0.035f
        const val MOTION_NOISE_MULTIPLIER = 3.4f
        const val MAX_TRACKED_NOISE_G = 0.28f

        const val FLAT_ARM_Z = 0.78f
        const val FLAT_MAX_ROLL = 0.46f
        const val FLAT_ARM_PITCH = 0.12f
        const val PITCH_ENTER_ANGLE = 0.20f
        const val PITCH_RETURN_ANGLE = 0.11f
        const val PITCH_RETURN_Z = 0.78f
        const val PITCH_MIN_ANGLE = 0.25f
        const val PITCH_FULL_ANGLE = 0.70f
        const val PITCH_MIN_RETURN_SPEED = 2.35f
        const val PITCH_FULL_RETURN_SPEED = 8f
        const val MIN_SHARP_RETURN_SECONDS = 0.025f
        const val MAX_SHARP_RETURN_SECONDS = 0.42f
        const val PITCH_TIMEOUT_SECONDS = 1.25f

        const val VERTICAL_MAX_Z = 0.48f
        const val VERTICAL_MIN_GRAVITY = 0.76f
        const val LIFT_START_G = 0.38f
        const val LIFT_END_G = 0.10f
        const val LIFT_MIN_PEAK_G = 0.62f
        const val LIFT_FULL_PEAK_G = 2.4f
        const val LIFT_MIN_IMPULSE = 0.018f
        const val LIFT_FULL_IMPULSE = 0.20f
        const val LIFT_MAX_SECONDS = 0.38f
    }
}
