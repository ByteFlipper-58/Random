package com.byteflipper.random.ui.lists

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.ui.common.defaultRandomItems
import com.byteflipper.random.utils.Constants.DEFAULT_DELAY_MS
import com.byteflipper.random.utils.Constants.DEFAULT_GENERATE_COUNT
import com.byteflipper.random.utils.Constants.INSTANT_DELAY_MS
import com.byteflipper.random.utils.Constants.MAX_GENERATE_COUNT
import com.byteflipper.random.utils.Constants.MIN_DELAY_MS
import com.byteflipper.random.utils.Constants.MIN_GENERATE_COUNT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.byteflipper.random.domain.lists.ListSortingMode as DomainListSortingMode
import com.byteflipper.random.domain.lists.usecase.GenerateListResultsUseCase
import com.byteflipper.random.domain.lists.usecase.SortListResultsUseCase
import com.byteflipper.random.ui.common.FeedbackUiEffect
import com.byteflipper.random.ui.common.GeneratorHostViewModel
import com.byteflipper.random.ui.lists.components.ListSortingMode
import dagger.hilt.android.qualifiers.ApplicationContext

data class ListUiState(
    val preset: ListPreset? = null,
    val editorItems: List<String> = emptyList(),
    val results: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val countText: String = DEFAULT_GENERATE_COUNT.toString(),
    val delayText: String = DEFAULT_DELAY_MS.toString(),
    val useDelay: Boolean = true,
    val allowRepetitions: Boolean = true,
    val usedItems: Set<String> = emptySet(),
    val showConfigDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showSaveDialog: Boolean = false,
    val saveName: String = "",
    val renameName: String = "",
    val openAfterSave: Boolean = true,
    val sortingMode: ListSortingMode = ListSortingMode.Random,
    val isOverlayVisible: Boolean = false,
    val cardColorSeed: Long? = null
)

sealed interface ListUiEvent {
    data class UpdateEditorItems(val items: List<String>) : ListUiEvent
    data class UpdateCountText(val text: String) : ListUiEvent
    data class UpdateDelayText(val text: String) : ListUiEvent
    data class UpdateUseDelay(val value: Boolean) : ListUiEvent
    data class UpdateAllowRepetitions(val value: Boolean) : ListUiEvent
    data object ToggleConfigDialog : ListUiEvent
    data object ToggleRenameDialog : ListUiEvent
    data class UpdateRenameName(val name: String) : ListUiEvent
    data object ToggleSaveDialog : ListUiEvent
    data class UpdateSaveName(val name: String) : ListUiEvent
    data class UpdateOpenAfterSave(val value: Boolean) : ListUiEvent
    data object ResetUsedItems : ListUiEvent
    data object ClearResults : ListUiEvent
    data class UpdateSortingMode(val mode: ListSortingMode) : ListUiEvent
    data class SetOverlayVisible(val visible: Boolean) : ListUiEvent
    data object RandomizeCardColor : ListUiEvent
}

@HiltViewModel
class ListViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val listPresetRepository: ListPresetRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val generateListResults: GenerateListResultsUseCase,
    private val sortListResults: SortListResultsUseCase,
    private val adsController: com.byteflipper.random.ads.AdsController
) : ViewModel(), GeneratorHostViewModel {
    companion object {
        private const val EDITOR_AUTOSAVE_DEBOUNCE_MS = 750L
    }

    private val presetId: Long? = savedStateHandle.get<Long?>("id")
        ?: savedStateHandle.get<String>("id")?.toLongOrNull()

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    private val _effects = kotlinx.coroutines.flow.MutableSharedFlow<ListUiEffect>()
    internal val effects: kotlinx.coroutines.flow.SharedFlow<ListUiEffect> = _effects

    val presets = listPresetRepository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var pendingSaveJob: Job? = null
    private var lastSavedItems: List<String> = emptyList()

    init {
        loadPreset()
    }
    fun onEvent(event: ListUiEvent) {
        when (event) {
            is ListUiEvent.UpdateEditorItems -> updateEditorItems(event.items)
            is ListUiEvent.UpdateCountText -> updateCountText(event.text)
            is ListUiEvent.UpdateDelayText -> updateDelayText(event.text)
            is ListUiEvent.UpdateUseDelay -> updateUseDelay(event.value)
            is ListUiEvent.UpdateAllowRepetitions -> updateAllowRepetitions(event.value)
            is ListUiEvent.ToggleConfigDialog -> toggleConfigDialog()
            is ListUiEvent.ToggleRenameDialog -> toggleRenameDialog()
            is ListUiEvent.UpdateRenameName -> updateRenameName(event.name)
            is ListUiEvent.ToggleSaveDialog -> toggleSaveDialog()
            is ListUiEvent.UpdateSaveName -> updateSaveName(event.name)
            is ListUiEvent.UpdateOpenAfterSave -> updateOpenAfterSave(event.value)
            is ListUiEvent.ResetUsedItems -> resetUsedItems()
            is ListUiEvent.ClearResults -> clearResults()
            is ListUiEvent.UpdateSortingMode -> updateSortingMode(event.mode)
            is ListUiEvent.SetOverlayVisible -> setOverlayVisible(event.visible)
            is ListUiEvent.RandomizeCardColor -> randomizeCardColor()
        }
    }


    private fun loadPreset() {
        viewModelScope.launch {
            if (presetId == null) {
                val defaultItems = settingsRepository.getDefaultListItems()

                val items = if (defaultItems.isEmpty()) {
                    appContext.defaultRandomItems()
                } else {
                    defaultItems
                }

                _uiState.update { state ->
                    state.copy(
                        editorItems = items.ifEmpty { listOf("") }
                    )
                }
                lastSavedItems = normalizeItems(items)
            } else {
                val preset = listPresetRepository.getById(presetId)
                preset?.let {
                    _uiState.update { state ->
                        state.copy(
                            preset = it,
                            editorItems = it.items.ifEmpty { listOf("") }
                        )
                    }
                    listPresetRepository.markUsed(it.id)
                    lastSavedItems = normalizeItems(it.items)
                }
            }
        }
    }

    fun updateEditorItems(items: List<String>) {
        _uiState.update { it.copy(editorItems = items) }
        scheduleSaveCurrent()
    }

    fun updateCountText(text: String) {
        _uiState.update { it.copy(countText = text) }
    }

    fun updateDelayText(text: String) {
        _uiState.update { it.copy(delayText = text) }
    }

    fun updateUseDelay(useDelay: Boolean) {
        _uiState.update { it.copy(useDelay = useDelay) }
    }

    fun updateAllowRepetitions(allowRepetitions: Boolean) {
        _uiState.update { it.copy(allowRepetitions = allowRepetitions) }
    }

    fun toggleConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = !it.showConfigDialog) }
    }

    fun toggleRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = !it.showRenameDialog) }
    }

    fun updateRenameName(name: String) {
        _uiState.update { it.copy(renameName = name) }
    }

    fun toggleSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = !it.showSaveDialog) }
    }

    fun updateSaveName(name: String) {
        _uiState.update { it.copy(saveName = name) }
    }

    fun updateOpenAfterSave(openAfterSave: Boolean) {
        _uiState.update { it.copy(openAfterSave = openAfterSave) }
    }

    fun resetUsedItems() {
        _uiState.update { it.copy(usedItems = emptySet()) }
    }

    fun clearResults() {
        _uiState.update { it.copy(results = emptyList()) }
    }

    fun generate(): List<String> {
        val state = _uiState.value
        val base = getBaseItems()
        val count = state.countText.toIntOrNull()?.coerceIn(MIN_GENERATE_COUNT, MAX_GENERATE_COUNT) ?: DEFAULT_GENERATE_COUNT
        return generateListResults(
            GenerateListResultsUseCase.Params(
                baseItems = base,
                count = count,
                allowRepetitions = state.allowRepetitions,
                usedItems = state.usedItems
            )
        )
    }

    fun generateAndUpdateResults(): List<String> {
        val rawResults = generate()
        val results = applySorting(rawResults)
        _uiState.update { state ->
            state.copy(
                results = results,
                usedItems = if (!state.allowRepetitions && results.isNotEmpty()) {
                    state.usedItems + results
                } else {
                    state.usedItems
                }
            )
        }
        return results
    }

    private fun applySorting(input: List<String>): List<String> {
        val mode = when (_uiState.value.sortingMode) {
            ListSortingMode.Random -> DomainListSortingMode.Random
            ListSortingMode.AlphabeticalAZ -> DomainListSortingMode.AlphabeticalAZ
            ListSortingMode.AlphabeticalZA -> DomainListSortingMode.AlphabeticalZA
        }
        return sortListResults(SortListResultsUseCase.Params(input = input, mode = mode))
    }

    fun updateSortingMode(mode: ListSortingMode) {
        _uiState.update { it.copy(sortingMode = mode) }
    }

    override fun getEffectiveDelayMs(): Int {
        val state = _uiState.value
        return if (state.useDelay) {
            state.delayText.toIntOrNull()?.coerceIn(MIN_DELAY_MS, DEFAULT_DELAY_MS * 2) ?: DEFAULT_DELAY_MS
        } else {
            INSTANT_DELAY_MS
        }
    }

    fun getBaseItems(): List<String> {
        return normalizeItems(_uiState.value.editorItems)
    }

    fun canGenerate(): Boolean {
        return getBaseItems().isNotEmpty()
    }

    fun canResetUsedItems(): Boolean {
        return _uiState.value.usedItems.isNotEmpty()
    }

    fun renamePreset() {
        val state = _uiState.value
        val newName = state.renameName.trim()
        val preset = state.preset

        if (newName.isNotEmpty() && preset != null) {
            viewModelScope.launch {
                val updatedPreset = preset.copy(
                    name = newName,
                    updatedAt = System.currentTimeMillis()
                )
                listPresetRepository.upsert(updatedPreset)
                _uiState.update { it.copy(preset = updatedPreset, showRenameDialog = false) }
            }
        }
    }

    fun saveAsNewPreset(
        name: String,
        openAfterSave: Boolean,
        itemsOverride: List<String>? = null,
        onPresetCreated: (Long) -> Unit
    ) {
        val trimmedName = name.trim()
        val items = itemsOverride ?: getBaseItems()

        if (trimmedName.isNotEmpty() && items.isNotEmpty()) {
            viewModelScope.launch {
                val newId = listPresetRepository.upsert(ListPreset(name = trimmedName, items = items))
                _uiState.update {
                    it.copy(
                        showSaveDialog = false,
                        saveName = trimmedName,
                        openAfterSave = openAfterSave
                    )
                }
                if (openAfterSave) {
                    onPresetCreated(newId)
                }
            }
        }
    }

    fun getCurrentResults(): List<String> {
        return _uiState.value.results
    }

    fun flushPendingSave() {
        pendingSaveJob?.cancel()
        pendingSaveJob = null
        persistCurrentIfChanged()
    }

    private fun scheduleSaveCurrent() {
        pendingSaveJob?.cancel()
        pendingSaveJob = viewModelScope.launch {
            delay(EDITOR_AUTOSAVE_DEBOUNCE_MS)
            persistCurrentIfChanged()
        }
    }

    private fun persistCurrentIfChanged() {
        val items = getBaseItems()
        if (items == lastSavedItems) return

        pendingSaveJob = null
        lastSavedItems = items

        val state = _uiState.value
        if (presetId != null && state.preset != null) {
            val updatedPreset = state.preset.copy(
                items = items,
                updatedAt = System.currentTimeMillis()
            )
            viewModelScope.launch {
                listPresetRepository.upsert(updatedPreset)
                _uiState.update { it.copy(preset = updatedPreset) }
            }
        } else {
            viewModelScope.launch {
                settingsRepository.setDefaultListItems(items)
            }
        }
    }

    override fun notifyHapticPressIfEnabled() {
        if (settings.value.hapticsEnabled) {
            emitEffect(ListUiEffect.HapticPress(settings.value.hapticsIntensity))
        }
    }

    override fun setOverlayVisible(visible: Boolean) {
        _uiState.update { it.copy(isOverlayVisible = visible) }
    }

    override fun randomizeCardColor() {
        val newSeed = kotlin.random.Random.nextLong()
        _uiState.update { it.copy(cardColorSeed = newSeed) }
    }

    private fun emitEffect(effect: ListUiEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    override fun checkAd(activity: android.app.Activity) {
        adsController.onNumbersOrListsGenerated(activity)
    }

    override fun onCleared() {
        flushPendingSave()
        super.onCleared()
    }

    private fun normalizeItems(items: List<String>): List<String> {
        return items.map { it.trim() }.filter { it.isNotEmpty() }
    }
}

internal sealed interface ListUiEffect : FeedbackUiEffect {
    data class ShowSnackbar(val messageRes: Int) : ListUiEffect {
        override val snackbarMessageRes: Int = messageRes
        override val hapticIntensity: com.byteflipper.random.data.settings.HapticsIntensity? = null
    }

    data class HapticPress(val intensity: com.byteflipper.random.data.settings.HapticsIntensity) : ListUiEffect {
        override val snackbarMessageRes: Int? = null
        override val hapticIntensity: com.byteflipper.random.data.settings.HapticsIntensity = intensity
    }
}
