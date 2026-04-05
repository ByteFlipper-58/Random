package com.byteflipper.random.ui.wheel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class WheelUiState(
    val items: List<String> = listOf("Item 1", "Item 2", "Item 3"),
    val excludedIndices: Set<Int> = emptySet(),
    val noRepeats: Boolean = false,
    val spinDuration: Int = 5000,
    val lastResult: String? = null,
    val lastResultIndex: Int? = null,
    val isSpinning: Boolean = false,
    val targetRotation: Float = 0f,
    val showEditorSheet: Boolean = false,
    val showSettingsSheet: Boolean = false
)

@HiltViewModel
class WheelViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val listPresetRepository: ListPresetRepository,
    private val adsController: com.byteflipper.random.ads.AdsController
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    val presets: StateFlow<List<ListPreset>> = listPresetRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(WheelUiState())
    val uiState: StateFlow<WheelUiState> = _uiState.asStateFlow()

    fun onEvent(event: WheelUiEvent) {
        when (event) {
            is WheelUiEvent.UpdateItems -> updateItems(event.items)
            is WheelUiEvent.SetNoRepeats -> setNoRepeats(event.enabled)
            is WheelUiEvent.SetSpinDuration -> setSpinDuration(event.durationMs)
            is WheelUiEvent.LoadPreset -> loadPreset(event.preset)
            is WheelUiEvent.Spin -> spin()
            is WheelUiEvent.SetSpinning -> setSpinning(event.spinning)
            is WheelUiEvent.SetResult -> setResult(event.index)
            is WheelUiEvent.SetResultByRotation -> setResultByRotation(event.rotation)
            WheelUiEvent.Reset -> reset()
            WheelUiEvent.ToggleEditorSheet -> _uiState.update { it.copy(showEditorSheet = !it.showEditorSheet) }
            WheelUiEvent.ToggleSettingsSheet -> _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
        }
    }

    private fun updateItems(items: List<String>) {
        val currentItems = _uiState.value.items
        // Only reset excluded if items actually changed
        if (items != currentItems) {
            _uiState.update { it.copy(items = items, excludedIndices = emptySet(), lastResult = null, lastResultIndex = null) }
        }
    }

    private fun setNoRepeats(enabled: Boolean) {
        _uiState.update { it.copy(noRepeats = enabled) }
    }

    private fun setSpinDuration(durationMs: Int) {
        _uiState.update { it.copy(spinDuration = durationMs.coerceIn(3000, 16000)) }
    }

    private fun loadPreset(preset: ListPreset) {
        _uiState.update { 
            it.copy(
                items = preset.items, 
                excludedIndices = emptySet(), 
                lastResult = null, 
                lastResultIndex = null
            ) 
        }
    }

    fun spin(): Pair<Int, Float>? {
        val state = _uiState.value
        val availableIndices = state.items.indices.filter { it !in state.excludedIndices }
        
        if (availableIndices.isEmpty()) return null
        
        val winnerIndex = availableIndices[Random.nextInt(availableIndices.size)]
        
        // Calculate rotation: multiple full rotations + angle to land on winner
        val itemCount = state.items.size - state.excludedIndices.size
        val anglePerItem = 360f / itemCount
        
        // Find visual index of winner among visible items
        val visibleItems = state.items.indices.filter { it !in state.excludedIndices }
        val visualIndex = visibleItems.indexOf(winnerIndex)
        
        // Calculate rotation to land on winner sector
        // Formula in WheelScreen: sectorIndex = floor((360 - rotation) / anglePerItem)
        // So to get visualIndex: (360 - rotation) / anglePerItem = visualIndex
        // Therefore: rotation = 360 - visualIndex * anglePerItem
        // Add random offset within sector (10% to 90% of sector to stay safely inside)
        val sectorStartAngle = visualIndex * anglePerItem
        val randomInSector = anglePerItem * (0.1f + Random.nextFloat() * 0.8f)
        val adjustedAngle = sectorStartAngle + randomInSector
        val baseRotation = ((360f - adjustedAngle) % 360f + 360f) % 360f
        
        // Add multiple full rotations for effect
        val fullRotations = Random.nextInt(5, 10) * 360f
        
        val targetRotation = state.targetRotation + fullRotations + baseRotation
        
        _uiState.update { it.copy(targetRotation = targetRotation, isSpinning = true) }
        
        return Pair(winnerIndex, targetRotation)
    }

    private fun setSpinning(spinning: Boolean) {
        _uiState.update { it.copy(isSpinning = spinning) }
    }

    private fun setResult(index: Int) {
        val state = _uiState.value
        val result = state.items.getOrNull(index) ?: return
        
        val newExcluded = if (state.noRepeats) {
            state.excludedIndices + index
        } else {
            state.excludedIndices
        }
        
        _uiState.update { 
            it.copy(
                lastResult = result, 
                lastResultIndex = index, 
                excludedIndices = newExcluded,
                isSpinning = false
            ) 
        }
    }

    private fun setResultByRotation(rotation: Float) {
        val state = _uiState.value
        val visibleItems = state.items.filterIndexed { index, _ -> index !in state.excludedIndices }
        
        if (visibleItems.isEmpty()) return
        
        val itemCount = visibleItems.size
        val anglePerItem = 360f / itemCount
        
        // Используем ту же формулу, что и в WheelScreen для определения сектора
        val normalizedRotation = ((rotation % 360f) + 360f) % 360f
        val adjustedRotation = ((360f - normalizedRotation) % 360f + 360f) % 360f
        val sectorIndex = (adjustedRotation / anglePerItem).toInt() % itemCount
        
        val result = visibleItems.getOrNull(sectorIndex) ?: return
        
        // Находим оригинальный индекс в полном списке items
        val originalIndex = state.items.indexOf(result)
        if (originalIndex == -1) return
        
        val newExcluded = if (state.noRepeats) {
            state.excludedIndices + originalIndex
        } else {
            state.excludedIndices
        }
        
        _uiState.update { 
            it.copy(
                lastResult = result, 
                lastResultIndex = originalIndex, 
                excludedIndices = newExcluded,
                isSpinning = false
            ) 
        }
    }

    private fun reset() {
        _uiState.update { it.copy(excludedIndices = emptySet(), lastResult = null, lastResultIndex = null) }
    }

    fun saveAsPreset(name: String) {
        viewModelScope.launch {
            val items = _uiState.value.items
            val preset = ListPreset(
                id = 0,
                name = name,
                items = items
            )
            listPresetRepository.upsert(preset)
        }
    }

    fun checkAd(activity: android.app.Activity) {
        adsController.onWheelSpun(activity)
    }
}

sealed interface WheelUiEvent {
    data class UpdateItems(val items: List<String>) : WheelUiEvent
    data class SetNoRepeats(val enabled: Boolean) : WheelUiEvent
    data class SetSpinDuration(val durationMs: Int) : WheelUiEvent
    data class LoadPreset(val preset: ListPreset) : WheelUiEvent
    data object Spin : WheelUiEvent
    data class SetSpinning(val spinning: Boolean) : WheelUiEvent
    data class SetResult(val index: Int) : WheelUiEvent
    data class SetResultByRotation(val rotation: Float) : WheelUiEvent
    data object Reset : WheelUiEvent
    data object ToggleEditorSheet : WheelUiEvent
    data object ToggleSettingsSheet : WheelUiEvent
}

