package com.byteflipper.random.domain.dice.physics

import com.byteflipper.random.domain.physics.Vec3

/** What the UI can ask the tray to do. Submitted from any thread, applied on the GL thread. */
sealed interface DiceCommand {

    /** Throws [count] dice. Their results are read from the faces that physically come to rest. */
    data class Roll(val count: Int) : DiceCommand

    /** Lays the dice out at rest showing [values], for when the player changes how many are in play. */
    data class Arrange(val values: List<Int>) : DiceCommand

    /** How hard the device itself is being moved, in g and in view space, gravity already taken out. */
    data class Motion(val acceleration: Vec3) : DiceCommand

    /** A measured phone gesture; [strength] is its speed normalised to `0..1`. */
    data class Toss(val kind: DiceTossKind, val strength: Float) : DiceCommand

    /** Pick one die up at [target] and hold it with a physical spring. */
    data class BeginDrag(val index: Int, val target: Vec3) : DiceCommand

    /** Move the spring target; [velocity] is the finger's filtered world-space velocity. */
    data class Drag(val index: Int, val target: Vec3, val velocity: Vec3) : DiceCommand

    /** Release the selected die with the finger's last measured velocity. */
    data class EndDrag(val index: Int, val velocity: Vec3) : DiceCommand

    /** A short tap on one die gives only that die a compact physical hop. */
    data class TapDie(val index: Int) : DiceCommand

    /** Swap the quality tier. Goes through the queue so one thread owns the bodies. */
    data class Retune(val tuning: DiceEngineTuning) : DiceCommand

}

enum class DiceTossKind {
    TABLE_PITCH,
    VERTICAL_LIFT
}

/** Things the simulation wants the UI to react to. */
sealed interface DiceEngineEvent {

    /** Something landed; [strength] is its physical severity normalised to 0..1. */
    data class Impact(
        val strength: Float,
        val material: DiceImpactMaterial
    ) : DiceEngineEvent

    /** Every die has stopped; [values] are read from the upward physical faces. */
    data class Settled(val values: List<Int>) : DiceEngineEvent
}

enum class DiceImpactMaterial {
    FELT,
    RIM,
    DICE
}

enum class DicePhase {
    /** Nothing thrown yet, or the last throw has been cleared away. */
    IDLE,

    /** The far edge is dipping and returning before the dice are kicked upward. */
    WINDUP,

    /** Dice in the air, or on the felt but not yet finished. */
    ROLLING,

    /** All of them are physically asleep. */
    SETTLED
}

/**
 * The tray as the renderer sees it, in GL-thread-owned arrays it can hand almost straight to GL.
 *
 * Orientations are quaternions rather than matrices: building the model matrix is the renderer's job,
 * and the simulation has no business knowing what a matrix is. The arrays are reused between frames;
 * callers must consume them synchronously on the engine's render thread.
 */
class DiceSnapshot(
    val count: Int,
    val halfExtent: Float,
    /** Three floats per die: where its centre is. */
    val positions: FloatArray,
    /** Four floats per die, as `w, x, y, z`. */
    val orientations: FloatArray,
    /** The number currently facing up on each die. */
    val values: IntArray,
    val phase: DicePhase,
    /** Visual pitch of the tray around its near edge during a tap-throw wind-up. */
    val trayPitchRadians: Float,
    /** Die currently held by a finger, or -1. */
    val selectedIndex: Int,
    /** True when nothing is moving, so the renderer may stop drawing at full rate. */
    val settled: Boolean
) {

    companion object {
        val Initial = DiceSnapshot(
            count = 0,
            halfExtent = 0.5f,
            positions = FloatArray(0),
            orientations = FloatArray(0),
            values = IntArray(0),
            phase = DicePhase.IDLE,
            trayPitchRadians = 0f,
            selectedIndex = -1,
            settled = true
        )
    }
}
