package com.byteflipper.random.ui.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.utils.Constants.DEFAULT_DELAY_MS
import com.byteflipper.random.utils.Constants.DEFAULT_GENERATE_COUNT
import com.byteflipper.random.utils.Constants.MAX_GENERATE_COUNT
import com.byteflipper.random.utils.Constants.MIN_DELAY_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

data class NumbersUiState(
    val fromText: String = "1",
    val toText: String = "10",
    val countText: String = DEFAULT_GENERATE_COUNT.toString(),
    val delayText: String = DEFAULT_DELAY_MS.toString(),
    val allowRepetitions: Boolean = true,
    val useDelay: Boolean = true,
    val usedNumbers: Set<Int> = emptySet(),
    val showConfigDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val frontValues: List<Int> = emptyList(),
    val backValues: List<Int> = emptyList()
)

@HiltViewModel
class NumbersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NumbersUiState())
    val uiState: StateFlow<NumbersUiState> = _uiState.asStateFlow()

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = runBlocking { settingsRepository.settingsFlow.first() }
    )

    fun updateFromText(text: String) {
        _uiState.update { it.copy(fromText = text) }
    }

    fun updateToText(text: String) {
        _uiState.update { it.copy(toText = text) }
    }

    fun updateCountText(text: String) {
        _uiState.update { it.copy(countText = text) }
    }

    fun updateDelayText(text: String) {
        _uiState.update { it.copy(delayText = text) }
    }

    fun updateAllowRepetitions(allowRepetitions: Boolean) {
        _uiState.update { it.copy(allowRepetitions = allowRepetitions) }
    }

    fun updateUseDelay(useDelay: Boolean) {
        _uiState.update { it.copy(useDelay = useDelay) }
    }

    fun toggleConfigDialog() {
        _uiState.update { it.copy(showConfigDialog = !it.showConfigDialog) }
    }

    fun toggleResetDialog() {
        _uiState.update { it.copy(showResetDialog = !it.showResetDialog) }
    }

    fun resetUsedNumbers() {
        _uiState.update { it.copy(usedNumbers = emptySet(), showResetDialog = false) }
    }

    fun clearResults() {
        _uiState.update { it.copy(frontValues = emptyList(), backValues = emptyList()) }
    }

    fun validateInputs(): Pair<IntRange, Int>? {
        val state = _uiState.value
        val from = state.fromText.trim().toIntOrNull()
        val to = state.toText.trim().toIntOrNull()
        val count = state.countText.trim().toIntOrNull() ?: 1

        if (from == null || to == null) return null
        if (count < 1) return null

        val range = if (from <= to) from..to else to..from
        val rangeSize = range.last - range.first + 1

        if (!state.allowRepetitions) {
            val availableNumbers = rangeSize - state.usedNumbers.size
            if (availableNumbers < count) return null
        }

        return Pair(range, count)
    }

    fun generate(): List<Int> {
        val validation = validateInputs()
        if (validation == null) return emptyList()

        val (range, count) = validation
        val state = _uiState.value

        return if (state.allowRepetitions) {
            List(count) { range.random() }
        } else {
            val availableNumbers = range.filter { it !in state.usedNumbers }
            availableNumbers.shuffled().take(count)
        }
    }

    fun generateAndUpdateResults(): List<Int> {
        val results = generate()
        if (results.isNotEmpty()) {
            val state = _uiState.value
            _uiState.update {
                it.copy(
                    frontValues = results,
                    backValues = results,
                    usedNumbers = if (!state.allowRepetitions) {
                        state.usedNumbers + results
                    } else {
                        state.usedNumbers
                    }
                )
            }
        }
        return results
    }

    fun getEffectiveDelayMs(): Int {
        val state = _uiState.value
        return if (state.useDelay) {
            state.delayText.toIntOrNull()?.coerceIn(MIN_DELAY_MS, DEFAULT_DELAY_MS * 20) ?: DEFAULT_DELAY_MS
        } else {
            1000
        }
    }

    fun canGenerate(): Boolean {
        return validateInputs() != null
    }

    fun canResetUsedNumbers(): Boolean {
        return _uiState.value.usedNumbers.isNotEmpty()
    }

    fun getAvailableNumbersCount(): Int {
        val validation = validateInputs()
        if (validation == null) return 0

        val (range, _) = validation
        val state = _uiState.value
        return if (state.allowRepetitions) {
            range.last - range.first + 1
        } else {
            range.count { it !in state.usedNumbers }
        }
    }
}
