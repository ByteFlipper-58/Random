package com.byteflipper.random.ui.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.numbers.SortingMode
import com.byteflipper.random.domain.numbers.usecase.GenerateNumbersUseCase
import com.byteflipper.random.domain.numbers.usecase.ValidateNumberInputsUseCase
import com.byteflipper.random.utils.Constants.DEFAULT_DELAY_MS
import com.byteflipper.random.utils.Constants.DEFAULT_GENERATE_COUNT
import com.byteflipper.random.utils.Constants.MIN_DELAY_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import com.byteflipper.random.R
import com.byteflipper.random.data.settings.HapticsIntensity

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
    val backValues: List<Int> = emptyList(),
    val sortingMode: SortingMode = SortingMode.Random,
    val isOverlayVisible: Boolean = false,
    val cardColorSeed: Long? = null
)

@HiltViewModel
class NumbersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val validateNumberInputs: ValidateNumberInputsUseCase,
    private val generateNumbers: GenerateNumbersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NumbersUiState())
    val uiState: StateFlow<NumbersUiState> = _uiState.asStateFlow()

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = runBlocking { settingsRepository.settingsFlow.first() }
    )

    private val _events = MutableSharedFlow<NumbersEvent>()
    val events: SharedFlow<NumbersEvent> = _events

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

    fun resetUsedNumbers() {
        _uiState.update { it.copy(usedNumbers = emptySet(), showResetDialog = false) }
        emitEvent(NumbersEvent.ShowSnackbar(R.string.history_cleared))
    }

    fun clearResults() {
        _uiState.update { it.copy(frontValues = emptyList(), backValues = emptyList()) }
    }

    fun validateInputs(): Pair<IntRange, Int>? {
        val state = _uiState.value
        return validateNumberInputs(
            ValidateNumberInputsUseCase.Params(
                fromText = state.fromText,
                toText = state.toText,
                countText = state.countText,
                allowRepetitions = state.allowRepetitions,
                usedNumbers = state.usedNumbers
            )
        )
    }

    fun generate(): List<Int> {
        val state = _uiState.value
        val validation = validateInputs() ?: return emptyList()
        val (range, count) = validation
        val result = generateNumbers(
            GenerateNumbersUseCase.Params(
                range = range,
                count = count,
                allowRepetitions = state.allowRepetitions,
                usedNumbers = state.usedNumbers,
                sortingMode = state.sortingMode
            )
        )
        _uiState.update { it.copy(usedNumbers = result.updatedUsedNumbers) }
        return result.values
    }

    fun getEffectiveDelayMs(): Int {
        val state = _uiState.value
        return if (state.useDelay) {
            state.delayText.toIntOrNull()?.coerceIn(MIN_DELAY_MS, DEFAULT_DELAY_MS * 20) ?: DEFAULT_DELAY_MS
        } else {
            1000
        }
    }

    fun updateSortingMode(mode: SortingMode) {
        _uiState.update { it.copy(sortingMode = mode) }
    }

    fun setFrontValues(values: List<Int>) {
        _uiState.update { it.copy(frontValues = values) }
    }

    fun setBackValues(values: List<Int>) {
        _uiState.update { it.copy(backValues = values) }
    }

    fun pruneUsedNumbersToRange(range: IntRange) {
        _uiState.update { it.copy(usedNumbers = it.usedNumbers.filter { v -> v in range }.toSet()) }
    }

    fun setConfigDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showConfigDialog = visible) }
    }

    fun notifyHapticPressIfEnabled() {
        if (settings.value.hapticsEnabled) {
            emitEvent(NumbersEvent.HapticPress(settings.value.hapticsIntensity))
        }
    }

    fun setOverlayVisible(visible: Boolean) {
        if (visible) {
            // assign a new color seed when opening overlay for stable color during session
            val seed = kotlin.random.Random.nextLong()
            _uiState.update { it.copy(isOverlayVisible = true, cardColorSeed = seed) }
        } else {
            _uiState.update { it.copy(isOverlayVisible = false) }
        }
    }

    fun randomizeCardColor() {
        val newSeed = kotlin.random.Random.nextLong()
        _uiState.update { it.copy(cardColorSeed = newSeed) }
    }

    private fun emitEvent(event: NumbersEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}

sealed interface NumbersEvent {
    data class ShowSnackbar(val messageRes: Int) : NumbersEvent
    data class HapticPress(val intensity: HapticsIntensity) : NumbersEvent
}
