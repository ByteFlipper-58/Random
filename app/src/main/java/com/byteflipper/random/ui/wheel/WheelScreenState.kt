package com.byteflipper.random.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.byteflipper.random.data.settings.WheelUsedSectorStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WINNER_FLASH_IN_MS = 110
private const val WINNER_FLASH_OUT_MS = 300

/** Pause after the color comes back, so the winner can be read before the sector leaves. */
private const val WINNER_HOLD_MS = 250L

private const val EXCLUDE_FADE_DURATION_MS = 320
private const val SECTOR_COLLAPSE_DURATION_MS = 420

internal class WheelScreenController(initialRotation: Float) {

    /**
     * Single source of the wheel rotation.
     *
     * A plain state rather than an [Animatable] for the sake of the drag gesture: it writes here
     * directly, without suspending. Keeping the rotation in an Animatable would require a `snapTo`
     * per gesture frame, which drops pointer events and makes the wheel stutter.
     */
    var rotation by mutableFloatStateOf(initialRotation)
        private set

    /** How grey the used sector is: 0 still colored, 1 fully grey. */
    val excludeFade = Animatable(0f)

    /** Share of the leaving sector: 1 full width, 0 gone. Starts full so nothing flickers. */
    val sectorCollapse = Animatable(1f)

    /** Strength of the winner highlight, 0..1. */
    val winnerHighlight = Animatable(0f)

    var showConfetti by mutableStateOf(false)
    var isDragging by mutableStateOf(false)

    var fadingIndex by mutableIntStateOf(-1)
        private set
    var highlightIndex by mutableIntStateOf(-1)
        private set

    /**
     * Sector whose removal has already been played out. Lets [pendingRemovalIndex] decide about
     * the ghost during composition, so the frame between exclusion and the start of the animation
     * does not show the wheel without the sector.
     */
    var completedRemovalIndex by mutableIntStateOf(-1)
        private set

    private val spinAnim = Animatable(initialRotation)
    private var removalJob: Job? = null

    // Parameters of the current removal, kept to finish the rotation shift if it is cut short.
    private var removalStartRotation = 0f
    private var removalSectorPosition = -1
    private var removalSectorCount = 0

    /** Rotation from the finger. Not suspending: called straight from the gesture handler. */
    fun rotateBy(deltaDegrees: Float) {
        rotation += deltaDegrees
    }

    fun normalizeRotation() {
        rotation = WheelGeometry.normalizeRotation(rotation)
    }

    suspend fun animateSpin(targetRotation: Float, spec: AnimationSpec<Float>) {
        spinAnim.snapTo(rotation)
        spinAnim.animateTo(targetRotation, spec) { rotation = value }
    }

    /**
     * Winner flash: the sector lightens sharply and returns to its own color.
     *
     * The highlight is not left hanging, because a permanent white tint reads as a washed out
     * sector rather than a selected one.
     */
    suspend fun flashWinner(index: Int) {
        highlightIndex = index
        winnerHighlight.snapTo(0f)
        winnerHighlight.animateTo(1f, tween(WINNER_FLASH_IN_MS, easing = LinearOutSlowInEasing))
        winnerHighlight.animateTo(0f, tween(WINNER_FLASH_OUT_MS, easing = FastOutSlowInEasing))
        highlightIndex = -1
    }

    /**
     * Removal of a used sector in the "remove from wheel" mode: flash, then grey, and only then
     * collapse to nothing while the neighbours close over it.
     */
    fun startRemoval(scope: CoroutineScope, index: Int, sectorPosition: Int, sectorCount: Int) {
        removalJob?.cancel()

        removalSectorPosition = sectorPosition
        removalSectorCount = sectorCount

        removalJob = scope.launch {
            flashWinner(index)
            delay(WINNER_HOLD_MS)

            fadingIndex = index
            excludeFade.animateTo(1f, tween(EXCLUDE_FADE_DURATION_MS))

            removalStartRotation = rotation
            sectorCollapse.animateTo(
                targetValue = 0f,
                animationSpec = tween(SECTOR_COLLAPSE_DURATION_MS, easing = FastOutSlowInEasing)
            ) {
                rotation = removalStartRotation + collapseRotationShift(value)
            }

            completedRemovalIndex = index
            resetSectorTransitions()
        }
    }

    /**
     * Back to the resting state: not grey and full width.
     *
     * Reset at the end of a cycle rather than at the start of the next one, otherwise the frame
     * between the stop and the start of the animation shows the sector already grey or collapsed.
     */
    private suspend fun resetSectorTransitions() {
        fadingIndex = -1
        excludeFade.snapTo(0f)
        sectorCollapse.snapTo(1f)
    }

    /**
     * How far to turn the wheel at the given [weight] of the leaving sector to keep its center in
     * place, so the neighbours converge on it symmetrically instead of the whole wheel sliding
     * sideways. Zero at weight 1, full shift at weight 0.
     */
    private fun collapseRotationShift(weight: Float): Float {
        if (removalSectorCount < 2 || removalSectorPosition < 0) return 0f

        val anchorAtFull = WheelGeometry.FULL_TURN *
            (removalSectorPosition + 0.5f) / removalSectorCount
        val anchorNow = WheelGeometry.FULL_TURN *
            (removalSectorPosition + weight / 2f) / (removalSectorCount - 1 + weight)

        return anchorAtFull - anchorNow
    }

    /** "Keep as grey" mode: the winner stays colored while its result is on screen. */
    fun holdExcludeFade(index: Int) {
        fadingIndex = index
    }

    /**
     * Finish pending transitions before a new spin.
     *
     * Required: the resting angle is computed from the layout without the used sector, so if that
     * sector has not finished collapsing the wheel would stop off target.
     */
    suspend fun settleBeforeSpin(pendingRemovalIndex: Int) {
        removalJob?.cancelAndJoin()
        removalJob = null

        if (highlightIndex >= 0) {
            winnerHighlight.snapTo(0f)
            highlightIndex = -1
        }

        if (pendingRemovalIndex >= 0) {
            // Jump to the exact final rotation, or the wheel twitches by the unfinished shift.
            rotation = removalStartRotation + collapseRotationShift(0f)
            completedRemovalIndex = pendingRemovalIndex
            resetSectorTransitions()
            return
        }

        if (fadingIndex >= 0) {
            // "Keep as grey" mode dims here: recoloring is unnoticeable once the wheel moves.
            excludeFade.animateTo(1f, tween(EXCLUDE_FADE_DURATION_MS))
            resetSectorTransitions()
        }
    }
}

@Composable
internal fun rememberWheelScreenController(initialRotation: Float): WheelScreenController {
    return remember { WheelScreenController(initialRotation) }
}

@Composable
internal fun rememberWheelVisibleItems(uiState: WheelUiState): List<String> {
    return remember(uiState.items, uiState.excludedIndices) {
        uiState.items.filterIndexed { index, _ -> index !in uiState.excludedIndices }
    }
}

/**
 * Layout used for drawing. Differs from the logical one in that the collapsing sector is still
 * there, otherwise there would be nothing left to animate.
 */
@Composable
internal fun rememberDrawnSectors(
    items: List<String>,
    excludedIndices: Set<Int>,
    usedSectorStyle: WheelUsedSectorStyle,
    ghostIndex: Int
): List<WheelSector> = remember(items, excludedIndices, usedSectorStyle, ghostIndex) {
    val sectors = wheelSectors(items, excludedIndices, usedSectorStyle)

    if (ghostIndex < 0 || sectors.any { it.index == ghostIndex }) {
        sectors
    } else {
        // Put the leaving sector back at its original position.
        (sectors + WheelSector(
            index = ghostIndex,
            label = items.getOrNull(ghostIndex).orEmpty(),
            isExcluded = true
        )).sortedBy { it.index }
    }
}

/**
 * Sector that is excluded but has not finished collapsing. Resolved during composition of the very
 * frame that excludes it, so the wheel never flashes the layout without it.
 */
internal fun pendingRemovalIndex(
    uiState: WheelUiState,
    completedRemovalIndex: Int
): Int {
    if (uiState.usedSectorStyle != WheelUsedSectorStyle.Remove) return -1

    val winnerIndex = uiState.lastResultIndex ?: return -1

    return if (winnerIndex in uiState.excludedIndices && winnerIndex != completedRemovalIndex) {
        winnerIndex
    } else {
        -1
    }
}

/**
 * Label of the sector under the pointer while the wheel spins.
 *
 * Returns a [State] instead of a ready string: the value is read where it is drawn, so spinning
 * recomposes only the result panel, and only when the sector actually changes.
 */
@Composable
internal fun rememberCurrentSectorText(
    sectors: List<WheelSector>,
    rotationProvider: () -> Float
): State<String> {
    val currentRotationProvider by rememberUpdatedState(rotationProvider)

    return remember(sectors) {
        derivedStateOf {
            if (sectors.isEmpty()) return@derivedStateOf ""
            val sectorPosition = WheelGeometry.sectorIndexAt(
                rotation = currentRotationProvider(),
                itemCount = sectors.size
            )
            sectors.getOrNull(sectorPosition)?.label.orEmpty()
        }
    }
}
