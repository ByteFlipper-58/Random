package com.byteflipper.random.ui.numbers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.ui.common.FlipGenerateScreenHost
import com.byteflipper.random.ui.common.rememberGeneratorScreenRuntime
import com.byteflipper.random.ui.common.rememberFlipGenerateController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.ui.components.ShakeEffect
import com.byteflipper.random.ui.numbers.components.NumbersConfigSheet
import com.byteflipper.random.ui.numbers.components.NumbersFabControls
import com.byteflipper.random.ui.numbers.components.NumbersFlipOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersScreen(onBack: () -> Unit) {
    val runtime = rememberGeneratorScreenRuntime()
    val view = LocalView.current
    val numbersClipboardLabel = stringResource(R.string.numbers_clipboard_label)

    // Все пользовательские параметры и результаты берём из VM
    val viewModel: NumbersViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val controller = rememberFlipGenerateController()

    NumbersScreenEffects(
        uiState = uiState,
        viewModel = viewModel,
        snackbarHostState = runtime.snackbarHostState,
        context = runtime.context,
        hapticsManager = runtime.hapticsManager
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    ShakeEffect(
        enabled = settings.shakeToGenerateEnabled,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onShake = {
            keyboardController?.hide()
            focusManager.clearFocus()
            controller.handleNumberGeneration(
                uiState = uiState,
                viewModel = viewModel,
                scope = runtime.scope,
                snackbarHostState = runtime.snackbarHostState,
                context = runtime.context,
                view = view
            )
        }
    )

    NumbersConfigSheet(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )

    NumbersScaffold(
        onBack = onBack,
        snackbarHostState = runtime.snackbarHostState,
        floatingActionButton = {
            NumbersFabControls(
                size = settings.fabSize,
                onConfigClick = { viewModel.onEvent(NumbersUiEvent.SetConfigDialogVisible(true)) },
                onGenerateClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    controller.handleNumberGeneration(
                        uiState = uiState,
                        viewModel = viewModel,
                        scope = runtime.scope,
                        snackbarHostState = runtime.snackbarHostState,
                        context = runtime.context,
                        view = view
                    )
                },
                onFabPositioned = { center, _ ->
                    controller.fabCenterInRoot = center
                }
            )
        }
    ) { innerPadding ->
        FlipGenerateScreenHost(
            innerPadding = innerPadding,
            controller = controller,
            content = { contentModifier, innerPadding ->
                NumbersContent(
                    modifier = contentModifier.padding(innerPadding),
                    fromText = uiState.fromText,
                    toText = uiState.toText,
                    onFromChange = { viewModel.onEvent(NumbersUiEvent.UpdateFromText(it)) },
                    onToChange = { viewModel.onEvent(NumbersUiEvent.UpdateToText(it)) }
                )
            },
            overlay = {
                NumbersFlipOverlay(
                uiState = uiState,
                settings = settings,
                flipCardState = controller.flipState,
                anchorInRoot = controller.fabCenterInRoot,
                context = runtime.context,
                snackbarHostState = runtime.snackbarHostState,
                hapticsManager = runtime.hapticsManager,
                clipboardLabel = numbersClipboardLabel,
                onEvent = viewModel::onEvent,
                onClosed = {
                    notifyNumbersOverlayClosed(
                        settings = settings,
                        hapticsManager = runtime.hapticsManager,
                        view = view
                    )
                },
                scope = runtime.scope,
                isGenerating = controller.isGenerating
            )
            }
        )
    }
}
