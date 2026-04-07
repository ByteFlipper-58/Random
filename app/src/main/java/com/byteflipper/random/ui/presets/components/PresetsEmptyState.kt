package com.byteflipper.random.ui.presets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.presets.PresetFilter
import com.byteflipper.random.ui.theme.ShapesTokens

@Composable
fun PresetsEmptyState(
    hasAnyPresets: Boolean,
    filter: PresetFilter,
    query: String,
    onCreatePreset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val titleRes = when {
        !hasAnyPresets -> R.string.no_presets_yet
        query.isNotBlank() -> R.string.no_search_results
        filter == PresetFilter.Recent -> R.string.no_recent_presets
        filter == PresetFilter.MostUsed -> R.string.no_most_used_presets
        else -> R.string.no_presets_found
    }
    val bodyText = when {
        !hasAnyPresets -> stringResource(R.string.presets_empty_create_hint)
        query.isNotBlank() -> stringResource(R.string.presets_empty_search_hint, query)
        filter == PresetFilter.Recent -> stringResource(R.string.presets_empty_recent_hint)
        filter == PresetFilter.MostUsed -> stringResource(R.string.presets_empty_most_used_hint)
        else -> stringResource(R.string.presets_empty_filtered_hint)
    }

    if (!hasAnyPresets) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Button(
                    onClick = onCreatePreset,
                    shape = ShapesTokens.MediumShape,
                    modifier = Modifier.padding(top = 22.dp)
                ) {
                    Text(text = stringResource(R.string.create_preset))
                }
            }
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = ShapesTokens.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
