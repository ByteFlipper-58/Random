package com.byteflipper.random.ui.lists

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.ShakeEffect
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.ui.common.FlipGenerateScreenHost
import com.byteflipper.random.ui.common.rememberGeneratorScreenRuntime
import com.byteflipper.random.ui.common.rememberFlipGenerateController
import com.byteflipper.random.ui.lists.components.ListConfigSheet
import com.byteflipper.random.ui.lists.components.ListFabControls
import com.byteflipper.random.ui.lists.components.ListFlipOverlay
import com.byteflipper.random.ui.lists.components.ListRenameDialog
import com.byteflipper.random.ui.lists.components.ListSaveDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onBack: () -> Unit,
    presetId: Long? = null,
    initialPreset: ListPreset? = null,
    onOpenListById: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    val runtime = rememberGeneratorScreenRuntime()
    val listString = stringResource(R.string.list)
    val listClipboardLabel = stringResource(R.string.list_clipboard_label)

    val controller = rememberListScreenController()
    val generateController = rememberFlipGenerateController()

    ListScreenEffects(
        uiState = uiState,
        controller = controller,
        viewModel = viewModel,
        onOpenListById = onOpenListById,
        snackbarHostState = runtime.snackbarHostState,
        context = runtime.context,
        hapticsManager = runtime.hapticsManager
    )

    // Shake-to-generate integration
    ShakeEffect(
        enabled = settings.shakeToGenerateEnabled,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onShake = {
            generateController.handleListGeneration(
                uiState = uiState,
                viewModel = viewModel,
                scope = runtime.scope,
                snackbarHostState = runtime.snackbarHostState,
                context = runtime.context
            )
        }
    )

    val displayedPreset = uiState.preset ?: initialPreset?.takeIf { it.id == presetId }
    val displayedItems = if (uiState.editorItems.isNotEmpty()) uiState.editorItems else (displayedPreset?.items ?: emptyList())
    val topTitle = controller.buildTopTitle(
        presetId = presetId,
        displayedPreset = displayedPreset,
        fallbackTitle = listString
    )
    val topSave = if (presetId == null) ({
        controller.handleTopSave(viewModel, listString)
    }) else null
    val topSaveResults = if (uiState.results.isNotEmpty()) ({
        controller.handleTopSaveResults(
            viewModel = viewModel,
            resultsPresetName = runtime.context.getString(R.string.results_preset_name)
        )
    }) else null
    val topRename = if (presetId != null) ({ viewModel.updateRenameName(displayedPreset?.name ?: listString); viewModel.toggleRenameDialog() }) else null

    ListScaffold(
        onBack = onBack,
        title = topTitle,
        presets = presets,
        onAddItems = viewModel::addItems,
        onShowSave = topSave,
        onShowSaveResults = topSaveResults,
        onShowRename = topRename,
        snackbarHostState = runtime.snackbarHostState,
        floatingActionButton = {
            ListFabControls(
                size = settings.fabSize,
                onConfigClick = { viewModel.toggleConfigDialog() },
                onGenerateClick = {
                    generateController.handleListGeneration(
                        uiState = uiState,
                        viewModel = viewModel,
                        scope = runtime.scope,
                        snackbarHostState = runtime.snackbarHostState,
                        context = runtime.context
                    )
                },
                onFabPositioned = { center, _ ->
                    generateController.fabCenterInRoot = center
                }
            )
        }
    ) { inner ->
        FlipGenerateScreenHost(
            innerPadding = inner,
            controller = generateController,
            content = { contentModifier, innerPadding ->
                if (presetId == null || displayedPreset != null) {
                    ListContent(
                        modifier = contentModifier,
                        contentPadding = innerPadding,
                        items = displayedItems,
                        onItemsChange = { viewModel.onEvent(ListUiEvent.UpdateEditorItems(it)) }
                    )
                } else {
                    Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                }
            },
            overlay = {
                ListFlipOverlay(
                    uiState = uiState,
                    settings = settings,
                    flipCardState = generateController.flipState,
                    anchorInRoot = generateController.fabCenterInRoot,
                    context = runtime.context,
                    snackbarHostState = runtime.snackbarHostState,
                    hapticsManager = runtime.hapticsManager,
                    clipboardLabel = listClipboardLabel,
                    onEvent = viewModel::onEvent,
                    onClosed = {},
                    scope = runtime.scope,
                    isGenerating = generateController.isGenerating
                )
            },
            dialogs = {
                ListConfigSheet(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )

                if (uiState.showRenameDialog && presetId != null) {
                    ListRenameDialog(
                        currentName = uiState.renameName,
                        onDismiss = { viewModel.onEvent(ListUiEvent.ToggleRenameDialog) },
                        onConfirm = { newName ->
                            viewModel.onEvent(ListUiEvent.UpdateRenameName(newName))
                            viewModel.renamePreset()
                        }
                    )
                }

                if (uiState.showSaveDialog) {
                    ListSaveDialog(
                        currentName = uiState.saveName,
                        presetCount = presets.size,
                        onDismiss = {
                            controller.handleSaveDialogDismiss(viewModel)
                        },
                        onConfirm = { name, shouldOpenAfterSave ->
                            controller.handleSaveConfirm(
                                viewModel = viewModel,
                                name = name,
                                shouldOpenAfterSave = shouldOpenAfterSave
                            )
                        }
                    )
                }
            }
        )
    }
}


