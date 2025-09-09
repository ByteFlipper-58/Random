package com.byteflipper.random.ui.lot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.lot.usecase.GenerateMarkedIndicesUseCase
import com.byteflipper.random.domain.lot.usecase.ValidateLotInputsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.byteflipper.random.R
import androidx.compose.ui.graphics.Color
import kotlin.random.Random
import com.byteflipper.random.data.settings.HapticsIntensity
import kotlinx.coroutines.launch

data class LotUiState(
    val totalText: String = "10",
    val markedText: String = "3",
    val markedIndices: Set<Int> = emptySet(),
    val isOverlayVisible: Boolean = false,
    val cards: List<LotCard> = emptyList(),
    val fabMode: LotFabMode = LotFabMode.Randomize
)

sealed interface LotEvent {
    data class ShowSnackbar(val messageRes: Int) : LotEvent
    data class HapticPress(val intensity: HapticsIntensity) : LotEvent
}

sealed interface LotAction {
    data class TotalChanged(val value: String) : LotAction
    data class MarkedChanged(val value: String) : LotAction
    data class GenerateRequested(val availableColors: List<Color>) : LotAction
    data class CardClicked(val id: Int) : LotAction
    data object RevealAll : LotAction
    data object Shuffle : LotAction
    data object OverlayDismissed : LotAction
}

@HiltViewModel
class LotViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val validateInputs: ValidateLotInputsUseCase,
    private val generateMarked: GenerateMarkedIndicesUseCase
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = runBlocking { settingsRepository.settingsFlow.first() }
    )

    private val _uiState = MutableStateFlow(LotUiState())
    val uiState: StateFlow<LotUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LotEvent>()
    val events: SharedFlow<LotEvent> = _events

    fun updateTotalText(text: String) {
        _uiState.value = _uiState.value.copy(totalText = text.filter { it.isDigit() })
    }

    fun updateMarkedText(text: String) {
        val filtered = text.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(markedText = filtered)
    }

    fun validate(): Pair<Int, Int>? {
        return validateInputs(ValidateLotInputsUseCase.Params(_uiState.value.totalText, _uiState.value.markedText))
    }

    fun generate() {
        val v = validate() ?: return
        val (total, marked) = v
        val indices = generateMarked(total, marked)
        _uiState.value = _uiState.value.copy(markedIndices = indices, isOverlayVisible = true)
    }

    fun revealAll() {
        val revealed = _uiState.value.cards.map { it.copy(isRevealed = true) }
        _uiState.value = _uiState.value.copy(cards = revealed, fabMode = LotFabMode.Randomize, isOverlayVisible = true)
    }

    fun reshuffle() {
        val shuffled = _uiState.value.cards.shuffled(Random)
        val hasMarked = shuffled.any { it.isMarked }
        _uiState.value = _uiState.value.copy(
            cards = shuffled.map { it.copy(isRevealed = false) },
            fabMode = if (hasMarked) LotFabMode.RevealAll else LotFabMode.Randomize
        )
    }

    fun hideOverlay() {
        _uiState.value = _uiState.value.copy(
            isOverlayVisible = false,
            cards = emptyList(),
            fabMode = LotFabMode.Randomize
        )
    }

    fun process(action: LotAction) {
        when (action) {
            is LotAction.TotalChanged -> updateTotalText(action.value)
            is LotAction.MarkedChanged -> updateMarkedText(action.value)
            is LotAction.GenerateRequested -> onGenerateRequested(action.availableColors)
            is LotAction.CardClicked -> onCardClicked(action.id)
            LotAction.RevealAll -> revealAll()
            LotAction.Shuffle -> reshuffle()
            LotAction.OverlayDismissed -> hideOverlay()
        }
    }

    private fun onGenerateRequested(availableColors: List<Color>) {
        val validated = validate()
        if (validated == null) {
            // отправим снэкбар
            emitEvent(LotEvent.ShowSnackbar(R.string.minimum_3_fields))
            return
        }
        val (total, _) = validated
        // пересчёт отмеченных индексов
        generate()

        val rows = computeRowSizes(total)
        val colors = distributeColorsSmartly(total, availableColors, rows)

        val markedIndices = _uiState.value.markedIndices
        val cards = List(total) { i ->
            LotCard(
                id = i,
                isMarked = i in markedIndices,
                isRevealed = false,
                color = colors[i]
            )
        }
        _uiState.value = _uiState.value.copy(cards = cards, fabMode = LotFabMode.RevealAll, isOverlayVisible = true)
    }

    private fun onCardClicked(id: Int) {
        val current = _uiState.value.cards
        val pos = current.indexOfFirst { it.id == id }
        if (pos == -1) return
        if (current[pos].isRevealed) return
        val wasMarked = current[pos].isMarked
        val updated = current.toMutableList().also { it[pos] = it[pos].copy(isRevealed = true) }
        _uiState.value = _uiState.value.copy(cards = updated)

        if (wasMarked && settings.value.hapticsEnabled) {
            emitEvent(LotEvent.HapticPress(settings.value.hapticsIntensity))
        }

        val totalMarked = updated.count { it.isMarked }
        val openedMarked = updated.count { it.isMarked && it.isRevealed }
        if (totalMarked > 0 && openedMarked >= totalMarked) {
            val allRevealed = updated.map { c -> if (!c.isMarked) c.copy(isRevealed = true) else c }
            _uiState.value = _uiState.value.copy(cards = allRevealed, fabMode = LotFabMode.Randomize)
        }
    }

    private fun emitEvent(event: LotEvent) {
        // fire-and-forget; ignoring backpressure since events are simple
        viewModelScope.launch { _events.emit(event) }
    }
}
