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
    var diceColors by mutableStateOf(List(maxDice) { diceColorPalette.random() })
    var isRolling by mutableStateOf(false)
    var currentRollJob by mutableStateOf<Job?>(null)

    fun syncDiceCount(count: Int) {
        diceCount = count
    }

    fun randomizeDiceColors() {
        diceColors = List(maxDice) { index ->
            val currentColor = diceColors[index]
            var newColor = diceColorPalette.random()
            while (newColor == currentColor && diceColorPalette.size > 1) {
                newColor = diceColorPalette.random()
            }
            newColor
        }
    }

    fun randomizeColorFor(index: Int) {
        val currentColor = diceColors[index]
        var newColor = diceColorPalette.random()
        while (newColor == currentColor && diceColorPalette.size > 1) {
            newColor = diceColorPalette.random()
        }
        diceColors = diceColors.toMutableList().also { it[index] = newColor }
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
            Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF2ECC71), Color(0xFFF39C12),
            Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFE67E22), Color(0xFF34495E),
            Color(0xFF16A085), Color(0xFF27AE60), Color(0xFF2980B9), Color(0xFF8E44AD),
            Color(0xFFC0392B), Color(0xFFD35400), Color(0xFF7F8C8D), Color(0xFF2C3E50)
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
