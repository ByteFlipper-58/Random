package com.byteflipper.random.ui.dice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.dice.usecase.RollDiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiceUiState(
    val diceCount: Int = 2,
    val values: List<Int> = listOf(1, 2),
    val isOverlayVisible: Boolean = false
)

@HiltViewModel
class DiceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val rollDice: RollDiceUseCase
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    private val _uiState = MutableStateFlow(DiceUiState())
    val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DiceUiEffect>()
    val effects: SharedFlow<DiceUiEffect> = _effects

    fun onEvent(event: DiceUiEvent) {
        when (event) {
            is DiceUiEvent.SetDiceCount -> setDiceCount(event.count)
            is DiceUiEvent.SetOverlayVisible -> setOverlayVisible(event.visible)
            is DiceUiEvent.RollAll -> rollAll()
            is DiceUiEvent.RollOne -> rollOne(event.index)
        }
    }

    fun setDiceCount(count: Int) {
        val clamped = count.coerceIn(1, 10)
        val base = _uiState.value.values.take(clamped)
        val padded = base + List(clamped - base.size) { 1 }
        _uiState.value = _uiState.value.copy(diceCount = clamped, values = padded)
    }

    fun setOverlayVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isOverlayVisible = visible)
    }

    fun rollAll(): List<Int> {
        val count = _uiState.value.diceCount
        val newValues = rollDice(count)
        _uiState.value = _uiState.value.copy(values = newValues)
        return newValues
    }

    fun rollOne(index: Int): Int {
        val count = _uiState.value.diceCount
        if (index !in 0 until count) return 1
        val value = rollDice(1).firstOrNull() ?: 1
        val newList = _uiState.value.values.toMutableList()
        if (newList.size < count) {
            while (newList.size < count) newList.add(1)
        }
        newList[index] = value
        _uiState.value = _uiState.value.copy(values = newList)
        return value
    }
}

sealed interface DiceUiEvent {
    data class SetDiceCount(val count: Int) : DiceUiEvent
    data class SetOverlayVisible(val visible: Boolean) : DiceUiEvent
    data object RollAll : DiceUiEvent
    data class RollOne(val index: Int) : DiceUiEvent
}

sealed interface DiceUiEffect
