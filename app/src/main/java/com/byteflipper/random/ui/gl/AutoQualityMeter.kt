package com.byteflipper.random.ui.gl

import com.byteflipper.random.domain.physics.SimulationQualityTier

/**
 * Watches frame times and names the quality tier, for when the player leaves the choice to us.
 *
 * Frame time is locked to the display's refresh, so it says nothing about how much headroom a device
 * has — only whether it is missing refreshes. A reading is therefore a verdict on the tier we are
 * *already* running, not a score: too slow and we give a tier back, comfortably fast and we try the
 * tier above and measure again. It stops after [MAX_WINDOWS] windows, because a meter that kept
 * promoting and demoting would flip between two tiers forever.
 *
 * Pure arithmetic on purpose: the renderer feeds it frame deltas from the GL thread, and everything
 * it decides can be checked without a GPU.
 */
class AutoQualityMeter(startTier: SimulationQualityTier) {

    private var tier = startTier
    private var frames = 0
    private var seconds = 0f
    private var windows = 0
    private var settledOn = false

    /** True while a window is still open, i.e. while the renderer must keep drawing at full rate. */
    val isMeasuring: Boolean get() = !settledOn

    /**
     * Takes one frame's duration and returns the tier to switch to, or null to carry on as is.
     *
     * [frameSeconds] is the wall-clock time the frame took; a value of zero (the very first frame)
     * counts towards the warm-up like any other.
     */
    fun observe(frameSeconds: Float): SimulationQualityTier? {
        if (settledOn) return null

        frames++
        // The opening frames pay for shader compilation and the first texture uploads, and would
        // read as a device that cannot keep up with anything.
        if (frames <= WARMUP_FRAMES) return null

        seconds += frameSeconds
        val measured = frames - WARMUP_FRAMES
        if (measured < WINDOW_FRAMES) return null

        val average = seconds / measured
        frames = WARMUP_FRAMES
        seconds = 0f
        windows++

        val next = when {
            average > SLOW_FRAME_SECONDS -> tier.oneCheaper()
            average < FAST_FRAME_SECONDS -> tier.oneRicher()
            else -> tier
        }
        // Nothing left to try in that direction, or the frame rate is where we want it: stop here.
        settledOn = windows >= MAX_WINDOWS || next == tier
        if (next == tier) return null
        tier = next
        return next
    }

    private companion object {
        const val WARMUP_FRAMES = 20

        /** Frames per reading — about three quarters of a second at 60 Hz. */
        const val WINDOW_FRAMES = 45

        /** At most two readings: one to judge the starting tier, one to judge the one we moved to. */
        const val MAX_WINDOWS = 2

        /** Slower than ~42 fps: the tier is too expensive for this device. */
        const val SLOW_FRAME_SECONDS = 0.024f

        /** Holding ~57 fps or better: there is room for the tier above. */
        const val FAST_FRAME_SECONDS = 0.0175f

        fun SimulationQualityTier.oneCheaper(): SimulationQualityTier = when (this) {
            SimulationQualityTier.HIGH -> SimulationQualityTier.BALANCED
            SimulationQualityTier.BALANCED -> SimulationQualityTier.BATTERY
            SimulationQualityTier.BATTERY -> SimulationQualityTier.BATTERY
        }

        fun SimulationQualityTier.oneRicher(): SimulationQualityTier = when (this) {
            SimulationQualityTier.HIGH -> SimulationQualityTier.HIGH
            SimulationQualityTier.BALANCED -> SimulationQualityTier.HIGH
            SimulationQualityTier.BATTERY -> SimulationQualityTier.BALANCED
        }
    }
}
