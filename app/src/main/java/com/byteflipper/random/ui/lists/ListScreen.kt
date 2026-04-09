package com.byteflipper.random.ui.lists

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.flip.FlipCardControls
import com.byteflipper.random.ui.components.flip.FlipCardOverlay
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.flip.rememberFlipCardState
import com.byteflipper.random.ui.lists.components.ListRenameDialog
import com.byteflipper.random.ui.lists.components.ListSaveDialog
import com.byteflipper.random.ui.lists.components.ListResultsDisplay
import com.byteflipper.random.ui.lists.components.ListFabControls
import com.byteflipper.random.ui.theme.getRainbowColors
import com.byteflipper.random.ui.settings.components.RadioOption
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.lists.components.ListSortingMode
import com.byteflipper.random.ui.theme.CardContentTheme
import kotlin.math.min
import kotlin.random.Random
import com.byteflipper.random.utils.findActivity
import androidx.compose.ui.platform.LocalContext
import com.byteflipper.random.ui.components.ShakeEffect
import com.byteflipper.random.data.preset.ListPreset


private fun Set<String>.indicesOf(baseSize: Int): Set<Int> {
    // Just a bounded placeholder set for the dialog. We don't need exact numbers UI for lists.
    return if (this.isEmpty()) emptySet() else (0 until kotlin.math.min(this.size, baseSize)).toSet()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onBack: () -> Unit,
    presetId: Long? = null,
    initialPreset: ListPreset? = null,
    onOpenListById: (Long) -> Unit = {}
) {
    val viewModel: ListViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    val listString = stringResource(R.string.list)
    val listEmptyText = stringResource(R.string.list_empty)
    val allOptionsUsedText = stringResource(R.string.all_options_used)
    val resetText = stringResource(R.string.reset)
    val listClipboardLabel = stringResource(R.string.list_clipboard_label)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hapticsManager = LocalHapticsManager.current

    var fabCenterInRoot by remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    var fabSize by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }
    var isGenerating by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pendingOpenPresetId by remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var saveDialogUsesResults by remember { androidx.compose.runtime.mutableStateOf(false) }

    val flipState = rememberFlipCardState()
    val flipCtrl = FlipCardControls(flipState)

    val ctx = LocalContext.current

    LaunchedEffect(uiState.showSaveDialog, pendingOpenPresetId) {
        val presetToOpen = pendingOpenPresetId
        if (!uiState.showSaveDialog && presetToOpen != null) {
            pendingOpenPresetId = null
            onOpenListById(presetToOpen)
        }
    }

    fun handleGenerate() {
        val base = viewModel.getBaseItems()
        if (base.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar(listEmptyText) }
            return
        }

        if (!uiState.allowRepetitions) {
            val pool = base.filter { it !in uiState.usedItems }.distinct()
            if (pool.isEmpty()) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = allOptionsUsedText,
                        actionLabel = resetText
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(ListUiEvent.ResetUsedItems)
                    }
                }
                return
            }
        }

        val delayMs = viewModel.getEffectiveDelayMs().toInt()
        if (!flipCtrl.isVisible()) {
            // назначаем цвет для текущего спина
            viewModel.onEvent(ListUiEvent.RandomizeCardColor)
            flipCtrl.open()
            viewModel.onEvent(ListUiEvent.SetOverlayVisible(true))
        }


        
        isGenerating = true
        flipCtrl.spinAndReveal(
            effectiveDelayMs = delayMs,
            onReveal = { _ ->
                isGenerating = false
                viewModel.generateAndUpdateResults()
            },
            onSpinCompleted = {
                viewModel.notifyHapticPressIfEnabled()
                // Реклама: каждая 8-я генерация списка
                // Реклама: каждая 8-я генерация списка
                ctx.findActivity()?.let { act -> viewModel.checkAd(act) }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ListUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(ctx.getString(effect.messageRes))
                is ListUiEffect.HapticPress -> hapticsManager?.performPress(effect.intensity)
            }
        }
    }

    // Shake-to-generate integration
    ShakeEffect(
        enabled = settings.shakeToGenerateEnabled,
        hapticsEnabled = settings.hapticsEnabled,
        hapticsIntensity = settings.hapticsIntensity,
        onShake = { handleGenerate() }
    )

    val displayedPreset = uiState.preset ?: initialPreset?.takeIf { it.id == presetId }
    val displayedItems = if (uiState.editorItems.isNotEmpty()) uiState.editorItems else (displayedPreset?.items ?: emptyList())
    val topTitle = if (presetId == null) stringResource(R.string.list) else (displayedPreset?.name ?: stringResource(R.string.list))
    val topSave = if (presetId == null) ({
        saveDialogUsesResults = false
        viewModel.updateSaveName(listString)
        viewModel.toggleSaveDialog()
    }) else null
    val topSaveResults = if (uiState.results.isNotEmpty()) ({
        saveDialogUsesResults = true
        viewModel.updateSaveName(ctx.getString(R.string.results_preset_name))
        viewModel.toggleSaveDialog()
    }) else null
    val topRename = if (presetId != null) ({ viewModel.updateRenameName(displayedPreset?.name ?: listString); viewModel.toggleRenameDialog() }) else null

    ListScaffold(
        onBack = onBack,
        title = topTitle,
        onShowSave = topSave,
        onShowSaveResults = topSaveResults,
        onShowRename = topRename,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ListFabControls(
                size = settings.fabSize,
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

            if (presetId == null || displayedPreset != null) {
                ListContent(
                    modifier = Modifier.fillMaxSize().padding(16.dp).blur(blur),
                    items = displayedItems,
                    onItemsChange = { viewModel.onEvent(ListUiEvent.UpdateEditorItems(it)) }
                )
            } else {
                Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
            }

            val rainbowColors = getRainbowColors()
            val animatedColor = remember { Animatable(Color.Transparent) }
            val targetColor = remember(uiState.cardColorSeed, uiState.results) {
                val r = uiState.cardColorSeed?.let { Random(it) } ?: Random
                rainbowColors[r.nextInt(rainbowColors.size)]
            }
            LaunchedEffect(targetColor) {
                if (animatedColor.value == Color.Transparent) {
                    animatedColor.snapTo(targetColor)
                } else {
                    animatedColor.animateTo(targetColor, tween(400))
                }
            }

            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.dp
            val screenHeightDp = configuration.screenHeightDp.dp
            
            // Limit card to screen size with some margins
            val maxCardWidth = (screenWidthDp - 32.dp).coerceAtLeast(200.dp)
            val maxCardHeight = (screenHeightDp - 64.dp).coerceAtLeast(300.dp)
            
            val effectiveCount = if (isGenerating) 1 else uiState.results.size

            // Use same base size logic as input numbers, or similar.
            // Since we don't have computeCardBaseSizeDp imported yet, let's replicate or import. 
            // Better to use a simpler logic for lists or just a fixed base. 
            // In NumbersScreen: base = 280 * scale. 
            
            val baseScale = when {
                 effectiveCount <= 10 -> 1.0
                 effectiveCount <= 25 -> 1.15
                 effectiveCount <= 50 -> 1.3
                 else -> 1.5
            }
            val basePx = (280 * baseScale).toInt()
            val dynamicMin = 240.coerceAtMost(maxCardWidth.value.toInt())
            val targetCardSize = basePx.coerceIn(dynamicMin, maxCardWidth.value.toInt()).dp
            
            val heightScale = when {
                effectiveCount <= 5 -> 1.0f
                effectiveCount <= 10 -> 1.2f
                effectiveCount <= 20 -> 1.4f
                effectiveCount <= 40 -> 1.6f
                effectiveCount <= 75 -> 1.8f
                else -> 2.2f
            }
            val minHeight = 300.dp.coerceAtMost(maxCardHeight)
            val targetCardHeight = (targetCardSize * heightScale).coerceIn(minHeight, maxCardHeight)

            val animatedCardSize by androidx.compose.animation.core.animateDpAsState(
                targetValue = targetCardSize,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
            )
            val animatedCardHeight by androidx.compose.animation.core.animateDpAsState(
                targetValue = targetCardHeight,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
            )

            FlipCardOverlay(
                state = flipState,
                anchorInRoot = fabCenterInRoot,
                onClosed = {
                    viewModel.onEvent(ListUiEvent.ClearResults)
                    viewModel.onEvent(ListUiEvent.SetOverlayVisible(false))
                },
                frontContainerColor = animatedColor.value,
                backContainerColor = animatedColor.value,
                cardSize = animatedCardSize,
                cardHeight = animatedCardHeight,
                onLongPress = {
                    // Копирование результатов в буфер обмена
                    if (uiState.results.isNotEmpty()) {
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = uiState.results.joinToString(", ")
                        clipboard.setPrimaryClip(ClipData.newPlainText(listClipboardLabel, text))
                        scope.launch {
                            snackbarHostState.showSnackbar(ctx.getString(R.string.copied_to_clipboard))
                        }
                        if (settings.hapticsEnabled) hapticsManager?.performPress(settings.hapticsIntensity)
                    }
                },
                frontContent = {
                    CardContentTheme {
                        ListResultsDisplay(
                            results = uiState.results,
                            cardColor = animatedColor.value,
                            cardSize = animatedCardHeight
                        )
                    }
                },
                backContent = {
                    CardContentTheme {
                        ListResultsDisplay(
                            results = uiState.results,
                            cardColor = animatedColor.value,
                            cardSize = animatedCardHeight
                        )
                    }
                }
            )

            if (uiState.showConfigDialog) {
                val sortOptions = listOf(
                    RadioOption(
                        key = ListSortingMode.Random.name,
                        title = stringResource(R.string.random_order),
                        icon = painterResource(id = R.drawable.shuffle_24px)
                    ),
                    RadioOption(
                        key = ListSortingMode.AlphabeticalAZ.name,
                        title = stringResource(R.string.alphabetical_az),
                        icon = painterResource(id = R.drawable.sort_by_alpha_24px)
                    ),
                    RadioOption(
                        key = ListSortingMode.AlphabeticalZA.name,
                        title = stringResource(R.string.alphabetical_za),
                        icon = painterResource(id = R.drawable.sort_by_alpha_24px)
                    )
                )
                GeneratorConfigDialog(
                    visible = uiState.showConfigDialog,
                    onDismissRequest = { viewModel.onEvent(ListUiEvent.ToggleConfigDialog) },
                    countText = uiState.countText,
                    onCountChange = { viewModel.onEvent(ListUiEvent.UpdateCountText(it)) },
                    allowRepetitions = uiState.allowRepetitions,
                    onAllowRepetitionsChange = { viewModel.onEvent(ListUiEvent.UpdateAllowRepetitions(it)) },
                    usedNumbers = uiState.usedItems.indicesOf(baseSize = 1_000_000),
                    availableRange = null,
                    onResetUsedNumbers = { viewModel.onEvent(ListUiEvent.ResetUsedItems) },
                    useDelay = uiState.useDelay,
                    onUseDelayChange = { viewModel.onEvent(ListUiEvent.UpdateUseDelay(it)) },
                    delayText = uiState.delayText,
                    onDelayChange = { viewModel.onEvent(ListUiEvent.UpdateDelayText(it)) },
                    sortingOptions = sortOptions,
                    selectedSortingKey = uiState.sortingMode.name,
                    onSortingChange = { key ->
                        val mode = ListSortingMode.valueOf(key)
                        viewModel.onEvent(ListUiEvent.UpdateSortingMode(mode))
                    }
                )
            }

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
                        saveDialogUsesResults = false
                        viewModel.onEvent(ListUiEvent.ToggleSaveDialog)
                    },
                    onConfirm = { name, shouldOpenAfterSave ->
                        viewModel.saveAsNewPreset(
                            name = name,
                            openAfterSave = shouldOpenAfterSave,
                            itemsOverride = if (saveDialogUsesResults) {
                                viewModel.getCurrentResults().takeIf { it.isNotEmpty() }
                            } else {
                                null
                            }
                        ) { newId ->
                            saveDialogUsesResults = false
                            pendingOpenPresetId = newId
                        }
                    }
                )
            }
        }
    }
}


