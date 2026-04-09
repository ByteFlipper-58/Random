package com.byteflipper.random.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import kotlin.math.absoluteValue
import kotlin.math.PI
import kotlin.math.sin

enum class HomeTab {
    Tools,
    Presets
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    selectionProgress: Float,
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSearch: (() -> Unit)? = null,
    searchTopBar: (@Composable () -> Unit)? = null,
    topBarOverride: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Column {
                when {
                    topBarOverride != null -> topBarOverride()
                    searchTopBar != null -> {
                        searchTopBar()
                        HomeTabSwitcherPlaceholder()
                    }
                    else -> {
                        HomeTopBar(
                            onOpenMenu = onOpenMenu,
                            onOpenSearch = onOpenSearch.takeIf { selectedTab == HomeTab.Presets }
                        )
                        HomeTabSwitcher(
                            selectionProgress = selectionProgress,
                            onSelectTab = onSelectTab
                        )
                    }
                }

                if (topBarOverride != null) {
                    HomeTabSwitcher(
                        selectionProgress = selectionProgress,
                        onSelectTab = onSelectTab
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@Composable
private fun HomeTabSwitcherPlaceholder() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(46.dp)
    )
}

@Composable
private fun HomeTabSwitcher(
    selectionProgress: Float,
    onSelectTab: (HomeTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
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
    val progress = selectionProgress.coerceIn(0f, (HomeTab.entries.size - 1).toFloat())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabCount = HomeTab.entries.size
            val slotWidth = maxWidth / tabCount
            val transitionFraction = (progress - progress.toInt()).absoluteValue
            val jellyProgress = sin((transitionFraction * PI).toFloat()).coerceIn(0f, 1f)
            val directionPull = (transitionFraction - 0.5f) * 2f
            val stretch = 30.dp * jellyProgress
            val indicatorWidth = slotWidth + stretch
            val trailingPull = 7.dp * directionPull * jellyProgress
            val rawOffset = slotWidth * progress - stretch / 2 + trailingPull
            val indicatorOffset = rawOffset.coerceIn(0.dp, maxWidth - indicatorWidth)
            val indicatorScaleX = 1f + 0.04f * jellyProgress
            val indicatorScaleY = 1f - 0.08f * jellyProgress
            val indicatorLift = (-1.5f * jellyProgress).dp

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset, y = indicatorLift)
                    .size(width = indicatorWidth, height = 38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(indicatorColor)
                    .graphicsLayer {
                        scaleX = indicatorScaleX
                        scaleY = indicatorScaleY
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeTab.entries.forEachIndexed { index, tab ->
                    val emphasis = (1f - (progress - index).absoluteValue).coerceIn(0f, 1f)
                    val softenedEmphasis = emphasis * emphasis * (3f - 2f * emphasis)
                    val textColor = lerp(unselectedTextColor, selectedTextColor, softenedEmphasis)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectTab(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (tab) {
                                HomeTab.Tools -> stringResource(R.string.tools)
                                HomeTab.Presets -> stringResource(R.string.presets)
                            },
                            color = textColor,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
