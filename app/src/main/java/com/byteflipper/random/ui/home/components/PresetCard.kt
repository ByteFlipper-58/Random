package com.byteflipper.random.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.byteflipper.random.ui.theme.ShapesTokens
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun PresetCard(
    preset: ListPreset,
    onPresetClick: (ListPreset) -> Unit,
    onPresetLongClick: ((ListPreset) -> Unit)? = null,
    onRenameClick: (ListPreset) -> Unit,
    onDeleteClick: (ListPreset) -> Unit,
    subtitle: String? = null,
    highlightPinned: Boolean = false,
    emphasize: Boolean = false,
    trailingContent: @Composable RowScope.(ListPreset) -> Unit = { currentPreset ->
        DefaultPresetCardActions(
            preset = currentPreset,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick
        )
    },
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val colorScheme = MaterialTheme.colorScheme
    val resolvedSubtitle = subtitle ?: stringResource(R.string.preset_items_count, preset.items.size)

    val cardElevation by animateDpAsState(
        targetValue = when {
            isPressed -> 1.dp
            emphasize -> 8.dp
            else -> 6.dp
        },
        animationSpec = tween(200),
        label = "elevation"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (emphasize) {
            lerp(colorScheme.surface, colorScheme.secondaryContainer, 0.18f)
        } else {
            colorScheme.surface
        },
        animationSpec = tween(250),
        label = "container_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (emphasize) {
            lerp(colorScheme.outlineVariant, colorScheme.tertiary, 0.45f)
        } else {
            colorScheme.outlineVariant
        },
        animationSpec = tween(250),
        label = "border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(ShapesTokens.CardShape),
        shape = ShapesTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = cardElevation
        ),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(ShapesTokens.CardShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.material3.ripple(bounded = true),
                    onClick = {
                        onPresetClick(preset)
                    },
                    onLongClick = onPresetLongClick?.let { callback ->
                        {
                            callback(preset)
                        }
                    }
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Иконка в контейнере
            Surface(
                shape = ShapesTokens.MediumShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.list_alt_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale)
                    )
                }
            }

            // Название и информация о пресете
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (highlightPinned && preset.isPinned) {
                        Icon(
                            painter = painterResource(id = R.drawable.keep_filled_24px),
                            contentDescription = stringResource(R.string.pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Можно добавить дополнительную информацию о пресете
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resolvedSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Кнопки действий
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trailingContent(preset)
            }
        }
    }
}

@Composable
private fun RowScope.DefaultPresetCardActions(
    preset: ListPreset,
    onRenameClick: (ListPreset) -> Unit,
    onDeleteClick: (ListPreset) -> Unit
) {
    FilledIconButton(
        onClick = { onRenameClick(preset) },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.edit_24px),
            contentDescription = stringResource(R.string.edit),
            modifier = Modifier.size(18.dp)
        )
    }

    FilledIconButton(
        onClick = { onDeleteClick(preset) },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete_24px),
            contentDescription = stringResource(R.string.delete),
            modifier = Modifier.size(18.dp)
        )
    }

    Icon(
        painter = painterResource(id = R.drawable.arrow_forward_ios_24px),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PresetCardPreview() {
    MaterialTheme {
        val samplePreset = ListPreset(
            id = 1L,
            name = stringResource(R.string.my_list),
            items = listOf(stringResource(R.string.item_1), stringResource(R.string.item_2), stringResource(R.string.item_3))
        )

        PresetCard(
            preset = samplePreset,
            onPresetClick = {},
            onRenameClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
