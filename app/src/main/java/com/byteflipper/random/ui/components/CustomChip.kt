package com.byteflipper.random.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.ui.theme.RandomDesignSystem

@Composable
fun CustomChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: Style = Style
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) {
        it.isSelected = selected
    }
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = 400f),
        label = "text_color"
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (selected) 8f else 2f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "shadow_elevation"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                clip = false,
                spotColor = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                } else {
                    Color.Black.copy(alpha = 0.15f)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .styleable(styleState, RandomDesignSystem.styles.customChip, style),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = if (selected) 0.3.sp else 0.sp
        )
    }
}
