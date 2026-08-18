package com.byteflipper.random.ui.dice

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job

internal class DiceScreenController(
    val maxDice: Int,
    val rotations: List<Animatable<Float, *>>,
    val scales: List<Animatable<Float, *>>,
    val scrimAlpha: Animatable<Float, *>,
    val diceColorPalette: List<Color>
) {
    var diceCount by mutableIntStateOf(2)
    var isAnimating by mutableStateOf(List(maxDice) { false })
    var diceColors by mutableStateOf(distinctColors())
    var isRolling by mutableStateOf(false)
    var currentRollJob by mutableStateOf<Job?>(null)

    fun syncDiceCount(count: Int) {
        diceCount = count
    }

    fun randomizeDiceColors() {
        diceColors = distinctColors()
    }

    fun randomizeColorFor(index: Int) {
        if (index !in diceColors.indices || diceColorPalette.isEmpty()) return
        val currentColor = diceColors[index]
        val usedByOthers = diceColors.filterIndexed { at, _ -> at != index }.toSet()
        val unused = diceColorPalette.filter { it != currentColor && it !in usedByOthers }
        val alternatives = unused.ifEmpty { diceColorPalette.filter { it != currentColor } }
        val newColor = alternatives.randomOrNull() ?: currentColor
        diceColors = diceColors.toMutableList().also { it[index] = newColor }
    }

    /** One pass through the whole palette before any colour repeats. */
    private fun distinctColors(): List<Color> {
        if (diceColorPalette.isEmpty()) return List(maxDice) { Color(0xFF52667A) }
        val shuffled = diceColorPalette.shuffled()
        return List(maxDice) { index -> shuffled[index % shuffled.size] }
    }

    fun normalizedRotation(index: Int): Float {
        val currentRotation = rotations[index].value
        return ((currentRotation % 360) / 90).toInt() * 90f
    }

    suspend fun openOverlayIfNeeded(
        isOverlayVisible: Boolean,
        onVisibilityChange: (Boolean) -> Unit
    ) {
        if (!isOverlayVisible) {
            onVisibilityChange(true)
            scrimAlpha.snapTo(0f)
            scrimAlpha.animateTo(
                targetValue = 1f,
                animationSpec = DiceAnimations.ScrimOpen
            )
        }
    }
}

@Composable
internal fun rememberDiceScreenController(): DiceScreenController {
    val maxDice = 10
    val diceColorPalette = remember {
        listOf(
            Color(0xFFD9415D), // crimson
            Color(0xFFF06449), // coral
            Color(0xFFD98F13), // amber
            Color(0xFF8A9A34), // olive
            Color(0xFF159A6C), // emerald
            Color(0xFF168C91), // teal
            Color(0xFF248AAF), // cyan
            Color(0xFF3478C9), // azure
            Color(0xFF5557C8), // indigo
            Color(0xFF7C4DCC), // violet
            Color(0xFFA142B8), // purple
            Color(0xFFC43E83), // magenta
            Color(0xFFD94A73), // rose
            Color(0xFF52667A), // slate
            Color(0xFF374151), // graphite
            Color(0xFF8A5A44)  // cocoa
        )
    }

    return remember {
        DiceScreenController(
            maxDice = maxDice,
            rotations = List(maxDice) { Animatable(0f) },
            scales = List(maxDice) { Animatable(1f) },
            scrimAlpha = Animatable(0f),
            diceColorPalette = diceColorPalette
        )
    }
}
