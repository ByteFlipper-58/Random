package com.byteflipper.random.ui.presets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.home.components.PresetCard
import com.byteflipper.random.ui.presets.components.PresetFiltersBar
import com.byteflipper.random.ui.presets.components.PresetManagerActions
import com.byteflipper.random.ui.presets.components.PresetSectionHeader
import com.byteflipper.random.ui.presets.components.PresetsEmptyState
import com.byteflipper.random.ui.teams.components.TeamPresetRow

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PresetsContentList(
    modifier: Modifier = Modifier,
    uiState: PresetsUiState,
    sections: List<PresetSection>,
    listState: LazyListState,
    selectionMode: Boolean,
    selectedPresetIds: Set<Long>,
    onFilterInteractionChanged: (Boolean) -> Unit,
    onFilterChange: (PresetFilter) -> Unit,
    onToggleSortOrder: () -> Unit,
    onCreatePreset: () -> Unit,
    onOpenPreset: (ListPreset) -> Unit,
    onOpenTeamPreset: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onRenameClick: (ListPreset) -> Unit,
    onDeleteClick: (ListPreset) -> Unit,
    onTogglePinned: (ListPreset) -> Unit,
    onPrepareCopy: (ListPreset) -> Unit,
    onPrepareExport: (ListPreset) -> Unit,
    onPrepareShare: (ListPreset) -> Unit,
    onDuplicate: (ListPreset, String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "presets_controls") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    PresetFiltersBar(
                        selectedFilter = uiState.filter,
                        sortAscending = uiState.sortAscending,
                        onFilterChange = onFilterChange,
                        onToggleSortOrder = onToggleSortOrder,
                        onInteractionChanged = onFilterInteractionChanged
                    )
                }
            }

            if (uiState.teamPresets.isNotEmpty()) {
                item(key = "teams_section_header") {
                    PresetSectionHeader(
                        title = stringResource(R.string.teams),
                        count = uiState.teamPresets.size,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(
                    items = uiState.teamPresets,
                    key = { preset -> "team_${preset.preset.id}" }
                ) { preset ->
                    TeamPresetRow(
                        item = preset,
                        onClick = { onOpenTeamPreset(preset.preset.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (uiState.presets.isEmpty() && uiState.teamPresets.isEmpty()) {
                item(key = "empty_state") {
                    PresetsEmptyState(
                        hasAnyPresets = uiState.hasAnyPresets,
                        filter = uiState.filter,
                        query = "",
                        onCreatePreset = onCreatePreset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            } else {
                sections.forEachIndexed { sectionIndex, section ->
                    if (section.titleRes != null) {
                        item(key = "section_${sectionIndex}_${section.titleRes}") {
                            PresetSectionHeader(
                                title = stringResource(section.titleRes),
                                count = section.presets.size,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    items(
                        items = section.presets,
                        key = { preset -> preset.id }
                    ) { preset ->
                        val duplicateName = stringResource(R.string.preset_duplicate_name, preset.name)
                        val metaText = presetMetaText(preset)
                        val isLastUsed = uiState.lastUsedPresetId == preset.id

                        PresetCard(
                            preset = preset,
                            onPresetClick = { currentPreset ->
                                if (selectionMode) {
                                    onToggleSelection(currentPreset.id)
                                } else {
                                    onOpenPreset(currentPreset)
                                }
                            },
                            onPresetLongClick = { currentPreset ->
                                if (!selectionMode || currentPreset.id !in selectedPresetIds) {
                                    onEnterSelection(currentPreset.id)
                                }
                            },
                            onRenameClick = onRenameClick,
                            onDeleteClick = onDeleteClick,
                            subtitle = metaText,
                            highlightPinned = true,
                            emphasize = isLastUsed || preset.id in selectedPresetIds,
                            trailingContent = { currentPreset ->
                                if (selectionMode) {
                                    Checkbox(
                                        checked = currentPreset.id in selectedPresetIds,
                                        onCheckedChange = { onToggleSelection(currentPreset.id) }
                                    )
                                } else {
                                    PresetManagerActions(
                                        preset = currentPreset,
                                        onTogglePinned = { onTogglePinned(currentPreset) },
                                        onCopy = { onPrepareCopy(currentPreset) },
                                        onExport = { onPrepareExport(currentPreset) },
                                        onShare = { onPrepareShare(currentPreset) },
                                        onDuplicate = { onDuplicate(currentPreset, duplicateName) },
                                        onRename = { onRenameClick(currentPreset) },
                                        onDelete = { onDeleteClick(currentPreset) }
                                    )
                                }
                            },
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = if (section.titleRes == null && sectionIndex == 0) 2.dp else 0.dp
                            )
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
