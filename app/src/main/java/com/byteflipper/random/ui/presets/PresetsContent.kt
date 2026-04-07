package com.byteflipper.random.ui.presets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.home.components.PresetCard
import com.byteflipper.random.ui.home.components.RenameListDialog
import com.byteflipper.random.ui.presets.components.PresetFiltersBar
import com.byteflipper.random.ui.presets.components.PresetManagerActions
import com.byteflipper.random.ui.presets.components.PresetSectionHeader
import com.byteflipper.random.ui.presets.components.PresetsEmptyState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val PresetItemPlacementSpring = spring<IntOffset>(
    dampingRatio = 0.84f,
    stiffness = Spring.StiffnessMediumLow
)

private data class PresetSection(
    val titleRes: Int?,
    val presets: List<ListPreset>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetsContent(
    modifier: Modifier = Modifier,
    onOpenPreset: (ListPreset) -> Unit,
    onCreatePreset: () -> Unit = {},
    onFilterInteractionChanged: (Boolean) -> Unit = {},
    viewModel: PresetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = uiState.firstVisibleItemScrollOffset
    )
    val sections = remember(uiState.presets, uiState.filter) {
        buildSections(uiState)
    }
    val context = LocalContext.current
    val hapticsManager = LocalHapticsManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var renameTarget by remember { mutableStateOf<ListPreset?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (firstVisibleItemIndex, firstVisibleItemScrollOffset) ->
                viewModel.updateListViewport(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
                )
            }
    }

    fun handleDeletePreset(preset: ListPreset) {
        scope.launch {
            viewModel.deletePreset(preset)
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.preset_deleted),
                actionLabel = context.getString(R.string.undo)
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePreset(preset)
            }
        }
    }

    fun performPresetHaptic() {
        if (settings.hapticsEnabled) {
            hapticsManager?.performPress(settings.hapticsIntensity)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            stickyHeader(key = "presets_controls") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    PresetFiltersBar(
                        selectedFilter = uiState.filter,
                        sortAscending = uiState.sortAscending,
                        onFilterChange = viewModel::updateFilter,
                        onToggleSortOrder = viewModel::toggleSortOrder,
                        onInteractionChanged = onFilterInteractionChanged
                    )
                }
            }

            if (uiState.presets.isEmpty()) {
                item(key = "empty_state") {
                    PresetsEmptyState(
                        hasAnyPresets = uiState.hasAnyPresets,
                        filter = uiState.filter,
                        query = "",
                        onCreatePreset = onCreatePreset,
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(horizontal = 16.dp)
                            .animateItem(
                                placementSpec = PresetItemPlacementSpring,
                                fadeInSpec = spring(
                                    dampingRatio = 0.88f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                fadeOutSpec = spring(
                                    dampingRatio = 0.95f,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                    )
                }
            } else {
                sections.forEachIndexed { sectionIndex, section ->
                    if (section.titleRes != null) {
                        item(key = "section_${sectionIndex}_${section.titleRes}") {
                            PresetSectionHeader(
                                title = stringResource(section.titleRes),
                                count = section.presets.size,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .animateItem(
                                        placementSpec = PresetItemPlacementSpring,
                                        fadeInSpec = spring(
                                            dampingRatio = 0.9f,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        fadeOutSpec = spring(
                                            dampingRatio = 0.95f,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
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
                            onPresetClick = onOpenPreset,
                            onRenameClick = { renameTarget = it },
                            onDeleteClick = ::handleDeletePreset,
                            subtitle = metaText,
                            highlightPinned = true,
                            emphasize = isLastUsed,
                            trailingContent = { currentPreset ->
                                PresetManagerActions(
                                    preset = currentPreset,
                                    onTogglePinned = {
                                        performPresetHaptic()
                                        viewModel.togglePinned(currentPreset)
                                    },
                                    onDuplicate = {
                                        scope.launch {
                                            val duplicatedPreset = viewModel.duplicatePreset(
                                                currentPreset,
                                                copyName = duplicateName
                                            )
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.preset_duplicated),
                                                actionLabel = context.getString(R.string.undo)
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.deletePreset(duplicatedPreset)
                                            }
                                        }
                                    },
                                    onRename = { renameTarget = currentPreset },
                                    onDelete = {
                                        performPresetHaptic()
                                        handleDeletePreset(currentPreset)
                                    }
                                )
                            },
                            modifier = Modifier
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = if (section.titleRes == null && sectionIndex == 0) 2.dp else 0.dp
                                )
                                .animateItem(
                                    placementSpec = PresetItemPlacementSpring,
                                    fadeInSpec = spring(
                                        dampingRatio = 0.88f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    fadeOutSpec = spring(
                                        dampingRatio = 0.92f,
                                        stiffness = Spring.StiffnessLow
                                    )
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

    if (renameTarget != null) {
        RenameListDialog(
            preset = renameTarget,
            onDismiss = { renameTarget = null },
            onRename = { preset, newName -> viewModel.renamePreset(preset, newName) },
            onPresetRenamed = { renameTarget = null }
        )
    }
}

private fun buildSections(
    uiState: PresetsUiState
): List<PresetSection> {
    if (uiState.presets.isEmpty()) return emptyList()

    return when (uiState.filter) {
        PresetFilter.All -> {
            val pinned = uiState.presets.filter { it.isPinned }
            val others = uiState.presets.filterNot { it.isPinned }
            buildList {
                if (pinned.isNotEmpty()) {
                    add(PresetSection(R.string.pinned, pinned))
                }
                if (others.isNotEmpty()) {
                    add(
                        PresetSection(
                            titleRes = if (pinned.isNotEmpty()) R.string.more_presets else null,
                            presets = others
                        )
                    )
                }
            }
        }

        PresetFilter.Recent -> listOf(PresetSection(null, uiState.presets))
        PresetFilter.MostUsed -> listOf(PresetSection(null, uiState.presets))
    }
}
