package com.byteflipper.random.ui.wheel.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

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

@Composable
internal fun rememberWheelTextPaint(itemCount: Int): android.graphics.Paint {
    return remember(itemCount) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            val shadowRadius = if (itemCount > 10) 2f else 4f
            setShadowLayer(shadowRadius, 1f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
        }
    }
}
