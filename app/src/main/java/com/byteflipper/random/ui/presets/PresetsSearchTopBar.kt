package com.byteflipper.random.ui.presets
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.home.components.PresetCard
import com.byteflipper.random.ui.presets.components.PresetFiltersBar
import com.byteflipper.random.ui.presets.components.PresetsEmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresetsSearchTopBar(
    uiState: PresetsUiState,
    onFilterChange: (PresetFilter) -> Unit,
    onToggleSortOrder: () -> Unit,
    onFilterInteractionChanged: (Boolean) -> Unit = {},
    onOpenPreset: (ListPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val searchBarState = rememberContainedSearchBarState(
        initialValue = SearchBarValue.Collapsed
    )
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val queryText by remember {
        derivedStateOf { textFieldState.text.toString() }
    }
    val searchResults by remember(uiState.availablePresets, queryText) {
        derivedStateOf {
            val normalizedQuery = queryText.trim()
            uiState.availablePresets.filter { preset ->
                normalizedQuery.isEmpty() || preset.name.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val colors = SearchBarDefaults.appBarWithSearchColors(
        searchBarColors = SearchBarDefaults.containedColors(state = searchBarState)
    )

    suspend fun collapseAndDismiss() {
        searchBarState.animateToCollapsed()
        withFrameNanos { }
        onDismiss()
    }

    LaunchedEffect(Unit) {
        searchBarState.animateToExpanded()
    }

    BackHandler {
        scope.launch {
            collapseAndDismiss()
        }
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            colors = colors.searchBarColors.inputFieldColors,
            onSearch = { },
            placeholder = { Text(stringResource(R.string.search_presets)) },
            leadingIcon = {
                IconButton(
                    onClick = {
                        scope.launch {
                            collapseAndDismiss()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            }
        )
    }

    AppBarWithSearch(
        state = searchBarState,
        inputField = inputField,
        colors = colors
    )

    ExpandedFullScreenContainedSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = colors.searchBarColors
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
                            onClick = {
                                scope.launch {
                                    searchBarState.animateToCollapsed()
                                    withFrameNanos { }
                                    onOpenPreset(preset)
                                    onDismiss()
                                }
                            }
                        )
                    }
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
