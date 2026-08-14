package com.byteflipper.random.ui.teams

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.confetti.ConfettiEffect
import com.byteflipper.confetti.ConfettiMode
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.common.FlipGenerateScreenHost
import com.byteflipper.random.ui.common.rememberFlipGenerateController
import com.byteflipper.random.ui.common.runGenerateSpin
import com.byteflipper.random.ui.teams.components.TeamPresetPickerSheet
import com.byteflipper.random.ui.teams.components.TeamSaveDialog
import com.byteflipper.random.ui.teams.components.TeamSettingsSheet
import com.byteflipper.random.ui.teams.components.TeamsFabControls
import com.byteflipper.random.ui.teams.components.TeamsFlipOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    viewModel: TeamsViewModel,
    onBack: () -> Unit,
    onManagePeople: () -> Unit,
    onPickMembers: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val controller = rememberFlipGenerateController()
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showPresetPickerSheet by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var saveName by rememberSaveable { mutableStateOf("") }
    var mainFabCenter by remember { mutableStateOf(Offset.Zero) }
    var showConfetti by remember { mutableStateOf(false) }
    val hapticsManager = LocalHapticsManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TeamsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = context.getString(effect.messageRes))
                }
            }
        }
    }

    fun launchGeneration() {
        keyboardController?.hide()
        focusManager.clearFocus()
        if (!viewModel.validateGeneration()) return
        controller.fabCenterInRoot = mainFabCenter
        controller.runGenerateSpin(
            effectiveDelayMs = viewModel.getEffectiveDelayMs(),
            onReveal = {
                viewModel.executeGeneration()
                if (settings.hapticsEnabled) {
                    hapticsManager?.performPress(settings.hapticsIntensity)
                }
            },
            onSpinCompleted = {
                showConfetti = true
            }
        )
    }

    TeamsScaffold(
        onBack = onBack,
        onOpenPreset = { showPresetPickerSheet = true },
        hasPresets = uiState.presets.isNotEmpty(),
        onManagePeople = onManagePeople,
        onSavePreset = {
            saveName = uiState.editor.name
            showSaveDialog = true
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            TeamsFabControls(
                size = settings.fabSize,
                onConfigClick = { showSettingsSheet = true },
                onGenerateClick = ::launchGeneration,
                onFabPositioned = { center -> mainFabCenter = center }
            )
        }
    ) { innerPadding ->
        FlipGenerateScreenHost(
            innerPadding = innerPadding,
            controller = controller,
            content = { modifier, innerPadding ->
                TeamsContent(
                    modifier = modifier,
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onPickMembers = onPickMembers,
                    onRemovePerson = viewModel::togglePersonSelection
                )
            },
            overlay = {
                TeamsFlipOverlay(
                    result = uiState.generation.result,
                    cardColorSeed = uiState.generation.cardColorSeed,
                    flipState = controller.flipState,
                    anchorInRoot = controller.fabCenterInRoot,
                    snackbarHostState = snackbarHostState,
                    onClosed = viewModel::clearGeneration,
                    isGenerating = controller.isGenerating
                )
            },
            dialogs = {
                TeamSettingsSheet(
                    visible = showSettingsSheet,
                    editorState = uiState.editor,
                    onDismiss = { showSettingsSheet = false },
                    onSplitModeChange = viewModel::setSplitMode,
                    onTeamCountChange = viewModel::updateTeamCountText,
                    onGroupSizeChange = viewModel::updateGroupSizeText,
                    onEqualTeamSizesOnlyChange = viewModel::updateEqualTeamSizesOnly,
                    onBalanceByGenderChange = viewModel::updateBalanceByGender,
                    onBalanceByAgeChange = viewModel::updateBalanceByAge,
                    onUseDelayChange = viewModel::updateUseDelay,
                    onDelayChange = viewModel::updateDelayText
                )

                TeamPresetPickerSheet(
                    visible = showPresetPickerSheet,
                    presets = uiState.presets,
                    onDismiss = { showPresetPickerSheet = false },
                    onOpenPreset = viewModel::openPreset
                )

                if (showSaveDialog) {
                    TeamSaveDialog(
                        currentName = saveName,
                        onNameChange = { saveName = it },
                        onDismiss = { showSaveDialog = false },
                        onConfirm = {
                            viewModel.savePreset(saveName)
                            showSaveDialog = false
                        }
                    )
                }
            }
        )
    }

    ConfettiEffect(
        trigger = showConfetti,
        mode = ConfettiMode.SIDE_CANNONS,
        particleCount = 300,
        durationMs = 3500L,
        onComplete = { showConfetti = false }
    )
}
