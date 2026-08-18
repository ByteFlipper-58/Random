package com.byteflipper.random.ui.dice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.byteflipper.random.domain.dice.physics.DiceEngine
import com.byteflipper.random.ui.dice.components.DiceTrayCountPicker
import com.byteflipper.random.ui.dice.gl.DiceGlSurface

/**
 * The 3D dice: a tray filling the screen, with the count picker floating over its far end.
 *
 * The tray's colours come from the theme rather than from a green baize, so it belongs to whatever
 * palette the device is wearing. The dice keep the flat ones' own palette — they are the same dice,
 * and having them change colour with the mode would say otherwise.
 */
@Composable
internal fun DiceTrayContent(
    engine: DiceEngine,
    dieColors: List<Color>,
    autoQuality: Boolean,
    diceCount: Int,
    onDiceCountChange: (Int) -> Unit,
    onRoll: () -> Unit,
    resultAnnouncement: String?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // The surface reaches under the bars on purpose: the gradient it draws is this screen's
        // background, and the tray is fitted to the whole viewport.
        DiceGlSurface(
            engine = engine,
            onRoll = onRoll,
            dieColors = dieColors,
            // Auto is the renderer's to measure; the other tiers are the player's word and stand.
            autoQuality = autoQuality,
            topColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            bottomColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            glowColor = MaterialTheme.colorScheme.primary,
            feltColor = MaterialTheme.colorScheme.secondaryContainer,
            rimColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            resultAnnouncement = resultAnnouncement,
            modifier = Modifier.fillMaxSize()
        )

        DiceTrayCountPicker(
            count = diceCount,
            onCountChange = onDiceCountChange,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(contentPadding)
                .padding(top = 8.dp)
        )
    }
}
