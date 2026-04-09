package com.byteflipper.random.ui.presets.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.presets.PresetFilter

private data class FilterMetrics(
    val leftPx: Int,
    val widthPx: Int
)

@Composable
fun PresetFiltersBar(
    selectedFilter: PresetFilter,
    sortAscending: Boolean,
    onFilterChange: (PresetFilter) -> Unit,
    onToggleSortOrder: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val metricsMap = remember { mutableStateMapOf<PresetFilter, FilterMetrics>() }
    val selectedMetrics = metricsMap[selectedFilter]
    var containerWidthPx by remember { mutableIntStateOf(0) }

    val containerColor = lerp(
        colorScheme.surfaceContainer,
        colorScheme.surfaceContainerHigh,
        0.55f
    )
    val indicatorColor = lerp(
        colorScheme.primaryContainer,
        colorScheme.tertiaryContainer,
        0.3f
    )
    val selectedTextColor = lerp(
        colorScheme.onPrimaryContainer,
        colorScheme.onTertiaryContainer,
        0.25f
    )
    val unselectedTextColor = colorScheme.onSurfaceVariant
    val sortLabel = when (selectedFilter) {
        PresetFilter.All -> stringResource(R.string.sort_name)
        PresetFilter.Recent -> stringResource(R.string.sort_activity)
        PresetFilter.MostUsed -> stringResource(R.string.sort_usage)
    }
    val orderDescription = if (sortAscending) {
        stringResource(R.string.ascending)
    } else {
        stringResource(R.string.descending)
    }
    val sortSummary = stringResource(
        R.string.sort_summary,
        sortLabel,
        orderDescription
    )

    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { (selectedMetrics?.leftPx ?: 0).toDp() },
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        ),
        label = "preset_filter_offset"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = with(density) { (selectedMetrics?.widthPx ?: 0).toDp() },
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "preset_filter_width"
    )

    LaunchedEffect(selectedFilter, selectedMetrics, containerWidthPx) {
        val metrics = selectedMetrics ?: return@LaunchedEffect
        if (containerWidthPx == 0) return@LaunchedEffect

        val selectedCenter = metrics.leftPx + (metrics.widthPx / 2)
        val targetScroll = (selectedCenter - (containerWidthPx / 2))
            .coerceIn(0, scrollState.maxValue)

        scrollState.animateScrollTo(targetScroll)
    }

    DisposableEffect(onInteractionChanged) {
        onDispose {
            onInteractionChanged(false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { containerWidthPx = it.width }
                    .clip(RoundedCornerShape(24.dp))
                    .background(containerColor)
                    .border(
                        width = 1.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .pointerInput(onInteractionChanged) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onInteractionChanged(true)
                            try {
                                do {
                                    awaitPointerEvent()
                                } while (currentEvent.changes.any { change -> change.pressed })
                            } finally {
                                onInteractionChanged(false)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(4.dp)
                ) {
                    if (selectedMetrics != null) {
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .requiredWidth(indicatorWidth)
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(indicatorColor)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PresetFilter.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val label = when (filter) {
                                PresetFilter.All -> stringResource(R.string.all)
                                PresetFilter.Recent -> stringResource(R.string.recent)
                                PresetFilter.MostUsed -> stringResource(R.string.sort_most_used)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .height(38.dp)
                                    .clickable { onFilterChange(filter) }
                                    .onGloballyPositioned { coordinates ->
                                        val position = coordinates.positionInParent()
                                        metricsMap[filter] = FilterMetrics(
                                            leftPx = position.x.toInt(),
                                            widthPx = coordinates.size.width
                                        )
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) selectedTextColor else unselectedTextColor,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            SortOrderButton(
                ascending = sortAscending,
                contentDescription = sortSummary,
                onClick = onToggleSortOrder
            )
        }

        Text(
            text = sortSummary,
            modifier = Modifier.padding(start = 6.dp, top = 8.dp),
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun SortOrderButton(
    ascending: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = lerp(
        colorScheme.secondaryContainer,
        colorScheme.tertiaryContainer,
        0.35f
    )
    val contentColor = lerp(
        colorScheme.onSecondaryContainer,
        colorScheme.onTertiaryContainer,
        0.35f
    )
    val rotation by animateFloatAsState(
        targetValue = if (ascending) 0f else 180f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "preset_sort_rotation"
    )
    val scale by animateFloatAsState(
        targetValue = if (ascending) 1f else 0.98f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "preset_sort_scale"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.arrow_upward_24px),
            contentDescription = null,
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation
            }
                .size(20.dp),
            tint = contentColor
        )
    }
}
