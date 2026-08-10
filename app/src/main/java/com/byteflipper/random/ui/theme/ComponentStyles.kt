package com.byteflipper.random.ui.theme

import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.border
import androidx.compose.foundation.style.contentPadding
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.scale
import androidx.compose.foundation.style.selected
import androidx.compose.ui.unit.dp

/** Visual defaults for custom components in the app design system. */
internal object RandomComponentStyles {
    val customChip = Style {
        val colors = LocalRandomColorScheme.currentValue

        shape(RoundedCornerShape(16.dp))
        background(colors.surfaceVariant.copy(alpha = 0.6f))
        border(1.dp, colors.outline.copy(alpha = 0.3f))
        contentPadding(horizontal = 10.dp, vertical = 6.dp)
        scale(1f)

        selected {
            animate(
                fromSpec = spring(stiffness = 400f),
                toSpec = spring(stiffness = 400f)
            ) {
                background(colors.primaryContainer)
                border(2.dp, colors.primary)
            }
            animate(
                fromSpec = spring(dampingRatio = 0.4f),
                toSpec = spring(dampingRatio = 0.4f)
            ) {
                scale(1.05f)
            }
        }

        pressed {
            animate(
                fromSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                toSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
            ) {
                scale(0.97f)
            }
        }
    }
}

/** Static access point for custom component styles. */
internal object RandomDesignSystem {
    val styles = RandomComponentStyles
}
