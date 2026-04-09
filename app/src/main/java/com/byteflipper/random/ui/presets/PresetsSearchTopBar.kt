package com.byteflipper.random.ui.presets

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import com.byteflipper.random.data.preset.ListPreset
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

    val inputField = rememberPresetSearchInputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        inputFieldColors = colors.searchBarColors.inputFieldColors,
        onDismiss = {
            scope.launch {
                collapseAndDismiss()
            }
        }
    )

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
        PresetsSearchResultsContent(
            uiState = uiState,
            queryText = queryText,
            searchResults = searchResults,
            onFilterChange = onFilterChange,
            onToggleSortOrder = onToggleSortOrder,
            onFilterInteractionChanged = onFilterInteractionChanged,
            onOpenPreset = { preset ->
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
