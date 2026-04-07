package com.byteflipper.random.ui.wheel.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.theme.ShapesTokens
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPresetSelectionSheet(
    preset: ListPreset,
    selectedIndices: Set<Int>,
    itemLimit: Int,
    onDismiss: () -> Unit,
    onSelectionChange: (Int) -> Unit,
    hapticsEnabled: Boolean,
    hapticsIntensity: HapticsIntensity,
    onConfirm: () -> Unit
) {
    val listState = rememberLazyListState()
    val hapticsManager = LocalHapticsManager.current
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val selectedCount = selectedIndices.size
    val canConfirm = selectedCount in 2..itemLimit
    val bottomBarOffset by animateDpAsState(
        targetValue = if (isBottomBarVisible) 0.dp else 164.dp,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 600f),
        label = "wheel_preset_bottom_bar_offset"
    )

    LaunchedEffect(listState) {
        var previousPosition = 0L
        snapshotFlow {
            listState.firstVisibleItemIndex.toLong() * 100_000L +
                listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { currentPosition ->
                when {
                    currentPosition <= 0L -> isBottomBarVisible = true
                    currentPosition > previousPosition -> isBottomBarVisible = false
                    currentPosition < previousPosition -> isBottomBarVisible = true
                }
                previousPosition = currentPosition
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(),
        sheetGesturesEnabled = false,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { WheelSheetDragHandle() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.wheel_preset_manual_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = lerp(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                            0.25f
                        )
                    ) {
                        Text(
                            text = stringResource(
                                R.string.wheel_preset_manual_message,
                                selectedCount,
                                itemLimit
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = lerp(
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                                0.2f
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 176.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = preset.items,
                            key = { index, item -> "${preset.id}_${index}_$item" }
                        ) { index, item ->
                            val isSelected = index in selectedIndices
                            val isEnabled = isSelected || selectedCount < itemLimit

                            WheelPresetSelectableItem(
                                index = index + 1,
                                label = item,
                                selected = isSelected,
                                enabled = isEnabled,
                                onClick = {
                                    val willSelect = !isSelected && selectedCount < itemLimit
                                    onSelectionChange(index)
                                    if (willSelect && selectedCount + 1 == itemLimit && hapticsEnabled) {
                                        hapticsManager?.performPress(hapticsIntensity)
                                    }
                                }
                            )
                        }
                    }

                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = bottomBarOffset),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (hapticsEnabled) {
                                    hapticsManager?.performPress(hapticsIntensity)
                                }
                                onConfirm()
                            },
                            enabled = canConfirm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(stringResource(R.string.wheel_add_to_wheel))
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPresetSelectableItem(
    index: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (selected) {
        lerp(colorScheme.primaryContainer, colorScheme.secondaryContainer, 0.2f)
    } else {
        colorScheme.surface
    }
    val borderColor = if (selected) {
        lerp(colorScheme.primary, colorScheme.outlineVariant, 0.45f)
    } else {
        colorScheme.outlineVariant.copy(alpha = 0.72f)
    }

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.48f
            },
        shape = ShapesTokens.CardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) colorScheme.primary else colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
