package com.byteflipper.random.ui.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.home.components.PresetCard
import com.byteflipper.random.ui.presets.components.PresetFiltersBar
import com.byteflipper.random.ui.presets.components.PresetsEmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberPresetSearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    inputFieldColors: TextFieldColors,
    onDismiss: () -> Unit
): @Composable () -> Unit {
    return {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            colors = inputFieldColors,
            onSearch = { },
            placeholder = { Text(stringResource(R.string.search_presets)) },
            leadingIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_back_24px),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            trailingIcon = {
                if (textFieldState.text.toString().isNotEmpty()) {
                    IconButton(
                        onClick = {
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_24px),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            }
        )
    }
}

@Composable
internal fun PresetsSearchResultsContent(
    uiState: PresetsUiState,
    queryText: String,
    searchResults: List<ListPreset>,
    onFilterChange: (PresetFilter) -> Unit,
    onToggleSortOrder: () -> Unit,
    onFilterInteractionChanged: (Boolean) -> Unit,
    onOpenPreset: (ListPreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        PresetFiltersBar(
            selectedFilter = uiState.filter,
            sortAscending = uiState.sortAscending,
            onFilterChange = onFilterChange,
            onToggleSortOrder = onToggleSortOrder,
            onInteractionChanged = onFilterInteractionChanged
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (searchResults.isEmpty()) {
            PresetsEmptyState(
                hasAnyPresets = uiState.hasAnyPresets,
                filter = uiState.filter,
                query = queryText
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = searchResults,
                    key = { preset -> preset.id }
                ) { preset ->
                    PresetSearchResultItem(
                        preset = preset,
                        isLastUsed = uiState.lastUsedPresetId == preset.id,
                        onClick = { onOpenPreset(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetSearchResultItem(
    preset: ListPreset,
    isLastUsed: Boolean,
    onClick: () -> Unit
) {
    PresetCard(
        preset = preset,
        onPresetClick = { onClick() },
        onRenameClick = {},
        onDeleteClick = {},
        subtitle = presetMetaText(preset),
        emphasize = isLastUsed,
        trailingContent = {},
        modifier = Modifier.fillMaxWidth()
    )
}
