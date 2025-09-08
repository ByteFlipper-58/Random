package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.EditorList
import com.byteflipper.random.ui.components.flip.FlipCardControls
import com.byteflipper.random.ui.components.flip.FlipCardOverlay
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.flip.rememberFlipCardState
import com.byteflipper.random.ui.lists.components.ListRenameDialog
import com.byteflipper.random.ui.lists.components.ListSaveDialog
import com.byteflipper.random.ui.lists.components.ListResultsDisplay
import com.byteflipper.random.ui.lists.components.ListFabControls
import com.byteflipper.random.ui.theme.getRainbowColors
import com.byteflipper.random.ui.components.RadioOption
import kotlinx.coroutines.launch



private fun Set<String>.indicesOf(baseSize: Int): Set<Int> {
    // Just a bounded placeholder set for the dialog. We don't need exact numbers UI for lists.
    return if (this.isEmpty()) emptySet() else (0 until kotlin.math.min(this.size, baseSize)).toSet()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(onBack: () -> Unit, presetId: Long? = null, onOpenListById: (Long) -> Unit = {}) {
    val viewModel: ListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    val listString = stringResource(R.string.list)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var fabCenterInRoot by remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    var fabSize by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

    val flipState = rememberFlipCardState()
    val flipCtrl = FlipCardControls(flipState)

    fun handleGenerate() {
        val base = viewModel.getBaseItems()
        if (base.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("List is empty") }
            return
        }

        if (!uiState.allowRepetitions) {
            val pool = base.filter { it !in uiState.usedItems }.distinct()
            if (pool.isEmpty()) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "All options used",
                        actionLabel = "Reset"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.resetUsedItems()
                    }
                }
                return
            }
        }

        val delayMs = viewModel.getEffectiveDelayMs().toInt()
        if (!flipCtrl.isVisible()) flipCtrl.open()

        flipCtrl.spinAndReveal(
            effectiveDelayMs = delayMs,
            onReveal = { _ ->
                val results = viewModel.generateAndUpdateResults()
            }
        )
    }

    val topTitle = if (presetId == null) stringResource(R.string.list) else (uiState.preset?.name ?: stringResource(R.string.list))
    val topSave = if (presetId == null) ({ viewModel.updateSaveName(listString); viewModel.toggleSaveDialog() }) else null
    val topRename = if (presetId != null) ({ viewModel.updateRenameName(uiState.preset?.name ?: ""); viewModel.toggleRenameDialog() }) else null

    ListScaffold(
        onBack = onBack,
        title = topTitle,
        onShowSave = topSave,
        onShowRename = topRename,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ListFabControls(
                onConfigClick = { viewModel.toggleConfigDialog() },
                onGenerateClick = { handleGenerate() },
                onFabPositioned = { center, size ->
                    fabCenterInRoot = center
                    fabSize = size
                }
            )
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            val blur = (8f * flipCtrl.scrimProgress.value).dp

            if (presetId == null || uiState.preset != null) {
                ListContent(
                    modifier = Modifier.fillMaxSize().padding(16.dp).blur(blur),
                    items = uiState.editorItems,
                    onItemsChange = { viewModel.updateEditorItems(it) }
                )
            } else {
                Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
            }

            val rainbowColors = getRainbowColors()
            val cardColor = androidx.compose.runtime.remember(uiState.results) { rainbowColors.random() }

            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val maxCardSide = (kotlin.math.min(configuration.screenWidthDp, configuration.screenHeightDp) - 64).dp
            val listCardSize = 320.dp.coerceAtMost(maxCardSide)

            val resultsCount = uiState.results.size
            val heightScale = when {
                resultsCount <= 5 -> 1.0f
                resultsCount <= 10 -> 1.2f
                resultsCount <= 20 -> 1.4f
                resultsCount <= 40 -> 1.6f
                else -> 1.8f
            }
            val listCardHeight = (listCardSize * heightScale).coerceIn(300.dp, maxCardSide)

            FlipCardOverlay(
                state = flipState,
                anchorInRoot = fabCenterInRoot,
                onClosed = { viewModel.clearResults() },
                frontContainerColor = cardColor,
                backContainerColor = cardColor,
                cardSize = listCardSize,
                cardHeight = listCardHeight,
                frontContent = {
                    ListResultsDisplay(
                        results = uiState.results,
                        cardColor = cardColor,
                        cardSize = listCardHeight
                    )
                },
                backContent = {
                    ListResultsDisplay(
                        results = uiState.results,
                        cardColor = cardColor,
                        cardSize = listCardHeight
                    )
                }
            )

            if (uiState.showConfigDialog) {
                val sortOptions = listOf(
                    RadioOption(
                        key = com.byteflipper.random.ui.lists.ListSortingMode.Random.name,
                        title = stringResource(R.string.random_order),
                        icon = rememberVectorPainter(Icons.Outlined.Shuffle)
                    ),
                    RadioOption(
                        key = com.byteflipper.random.ui.lists.ListSortingMode.AlphabeticalAZ.name,
                        title = stringResource(R.string.alphabetical_az),
                        icon = rememberVectorPainter(Icons.Outlined.SortByAlpha)
                    ),
                    RadioOption(
                        key = com.byteflipper.random.ui.lists.ListSortingMode.AlphabeticalZA.name,
                        title = stringResource(R.string.alphabetical_za),
                        icon = rememberVectorPainter(Icons.Outlined.SortByAlpha)
                    )
                )
                GeneratorConfigDialog(
                    visible = uiState.showConfigDialog,
                    onDismissRequest = { viewModel.toggleConfigDialog() },
                    countText = uiState.countText,
                    onCountChange = { viewModel.updateCountText(it) },
                    allowRepetitions = uiState.allowRepetitions,
                    onAllowRepetitionsChange = { viewModel.updateAllowRepetitions(it) },
                    usedNumbers = uiState.usedItems.indicesOf(baseSize = 1_000_000),
                    availableRange = null,
                    onResetUsedNumbers = { viewModel.resetUsedItems() },
                    useDelay = uiState.useDelay,
                    onUseDelayChange = { viewModel.updateUseDelay(it) },
                    delayText = uiState.delayText,
                    onDelayChange = { viewModel.updateDelayText(it) },
                    sortingOptions = sortOptions,
                    selectedSortingKey = uiState.sortingMode.name,
                    onSortingChange = { key ->
                        val mode = com.byteflipper.random.ui.lists.ListSortingMode.valueOf(key)
                        viewModel.updateSortingMode(mode)
                    }
                )
            }

            if (uiState.showRenameDialog && presetId != null) {
                ListRenameDialog(
                    currentName = uiState.renameName,
                    onDismiss = { viewModel.toggleRenameDialog() },
                    onConfirm = { newName ->
                        viewModel.updateRenameName(newName)
                        viewModel.renamePreset()
                    }
                )
            }

            if (uiState.showSaveDialog) {
                ListSaveDialog(
                    currentName = uiState.saveName,
                    presetCount = presets.size,
                    onDismiss = { viewModel.toggleSaveDialog() },
                    onConfirm = { name, shouldOpenAfterSave ->
                        viewModel.updateSaveName(name)
                        viewModel.updateOpenAfterSave(shouldOpenAfterSave)
                        viewModel.saveAsNewPreset { newId ->
                            if (shouldOpenAfterSave) {
                                onOpenListById(newId)
                            }
                        }
                    }
                )
            }
        }
    }
}


