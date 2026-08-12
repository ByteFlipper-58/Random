package com.byteflipper.random.ui.wheel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

internal val wheelColors = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFFD81B60),
    Color(0xFFFDD835),
    Color(0xFF5E35B1),
    Color(0xFF039BE5),
    Color(0xFF7CB342),
    Color(0xFFFF7043),
    Color(0xFF3949AB),
    Color(0xFF26A69A),
    Color(0xFFAB47BC),
    Color(0xFF66BB6A),
)

/**
 * Stroke widths and offsets of the wheel as fractions of its radius.
 *
 * Raw pixel values would render differently on every screen: a hairline rim on a dense display and
 * a three times too thick one on mdpi. Fractions also survive a change of the wheel size.
 *
 * The numbers are tuned to match a 320.dp wheel at ~2.75x density.
 */
internal object WheelDrawRatios {
    const val RIM_WIDTH = 0.041f
    const val RIM_OFFSET = 0.021f
    const val BORDER_OFFSET = 0.003f
    const val BORDER_WIDTH = 0.008f

    const val DIVIDER_INNER_RADIUS = 0.078f
    const val DIVIDER_INNER_RADIUS_DENSE = 0.065f
    const val DIVIDER_WIDTH = 0.005f
    const val DIVIDER_WIDTH_DENSE = 0.004f

    const val SHADOW_OFFSET_X = 0.0155f
    const val SHADOW_OFFSET_Y = 0.021f
    const val SHADOW_SPREAD = 0.039f

    const val HUB_SHADOW_OFFSET_X = 0.003f
    const val HUB_SHADOW_OFFSET_Y = 0.005f
}

/**
 * Pointer geometry as fractions of its own size, for the same reason.
 * Tuned for a 32x44.dp pointer at ~2.75x density.
 */
internal object WheelPointerRatios {
    const val EDGE_INSET_X = 0.034f
    const val SHOULDER_Y = 0.083f
    const val SHADOW_OFFSET_X = 0.034f
    const val HIGHLIGHT_INSET_X = 0.023f
    const val HIGHLIGHT_Y = 0.041f
    const val OUTLINE_WIDTH = 0.028f
    const val INNER_BOTTOM_Y = 0.066f
    const val INNER_INSET_X = 0.091f
    const val INNER_SHOULDER_Y = 0.099f
    const val INNER_TOP_Y = 0.033f
    const val INNER_WIDTH = 0.011f
}

@Composable
internal fun rememberWheelTextPaint(itemCount: Int): android.graphics.Paint {
    val density = LocalDensity.current

    return remember(itemCount, density) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            // In density independent units, or the shadow all but disappears on dense screens.
            val shadowRadius = with(density) { if (itemCount > 10) 0.75.dp.toPx() else 1.5.dp.toPx() }
            val shadowOffset = with(density) { 0.5.dp.toPx() }
            setShadowLayer(
                shadowRadius,
                shadowOffset,
                shadowOffset,
                android.graphics.Color.argb(180, 0, 0, 0)
            )
        }
    }
}
