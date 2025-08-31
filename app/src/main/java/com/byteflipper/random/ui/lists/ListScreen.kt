package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.EditorList
import com.byteflipper.random.ui.components.FlipCardControls
import com.byteflipper.random.ui.components.FlipCardOverlay
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.rememberFlipCardState
import com.byteflipper.random.ui.lists.components.ListRenameDialog
import com.byteflipper.random.ui.lists.components.ListSaveDialog
import com.byteflipper.random.ui.lists.components.ListResultsDisplay
import com.byteflipper.random.ui.lists.components.ListFabControls
import com.byteflipper.random.ui.theme.getRainbowColors
import kotlin.math.min
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

    // Получить строковые ресурсы заранее
    val listString = stringResource(R.string.list)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // FAB geometry
    var fabCenterInRoot by remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    var fabSize by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }

    // Flip card
    val flipState = rememberFlipCardState()
    val flipCtrl = FlipCardControls(flipState)

    // Обработка генерации
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
                // results уже обновлены в ViewModel
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (presetId == null) stringResource(R.string.list)
                        else (uiState.preset?.name ?: stringResource(R.string.list))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (presetId == null) {
                        IconButton(onClick = {
                            viewModel.updateSaveName(listString)
                            viewModel.toggleSaveDialog()
                        }) {
                            Icon(painterResource(R.drawable.save_24px), contentDescription = stringResource(R.string.save))
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.updateRenameName(uiState.preset?.name ?: "")
                            viewModel.toggleRenameDialog()
                        }) {
                            Icon(painterResource(R.drawable.edit_24px), contentDescription = stringResource(R.string.rename))
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            ListFabControls(
                onConfigClick = { viewModel.toggleConfigDialog() },
                onGenerateClick = { handleGenerate() },
                onFabPositioned = { center, size ->
                    fabCenterInRoot = center
                    fabSize = size
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            val blur = (8f * flipCtrl.scrimProgress.value).dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .blur(blur),
                verticalArrangement = Arrangement.Top
            ) {
                if (presetId == null || uiState.preset != null) {
                    EditorList(
                        items = androidx.compose.runtime.snapshots.SnapshotStateList<String>().apply {
                            clear()
                            addAll(uiState.editorItems)
                        },
                        onItemsChange = { viewModel.updateEditorItems(it) },
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        minItems = 1
                    )
                } else {
                    Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Получить цвета радуги и выбрать случайный для карточки
            val rainbowColors = getRainbowColors()
            val cardColor = remember(uiState.results) { rainbowColors.random() }

            // Адаптивный размер карточки для списков
            val listCardSize = 320.dp

            // Flip overlay
            FlipCardOverlay(
                state = flipState,
                anchorInRoot = fabCenterInRoot,
                onClosed = { viewModel.clearResults() },
                // Используем один и тот же цвет для обеих сторон карточки
                frontContainerColor = cardColor,
                backContainerColor = cardColor,
                cardSize = listCardSize,
                frontContent = {
                    ListResultsDisplay(
                        results = uiState.results,
                        cardColor = cardColor
                    )
                },
                backContent = {
                    ListResultsDisplay(
                        results = uiState.results,
                        cardColor = cardColor
                    )
                }
            )

            // Config dialog
            if (uiState.showConfigDialog) {
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
                    onDelayChange = { viewModel.updateDelayText(it) }
                )
            }

            // Rename dialog (only for saved presets)
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

            // Save as new preset dialog (for default screen)
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


