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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class LotUiState(
    val totalText: String = "10",
    val markedText: String = "3",
    val markedIndices: Set<Int> = emptySet(),
    val isOverlayVisible: Boolean = false
)

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
        // В UI раскрытие контролируется через локальную анимацию карточек, тут лишь флаг оверлея
        _uiState.value = _uiState.value.copy(isOverlayVisible = true)
    }

    fun reshuffle() {
        val v = validate() ?: return
        val (total, marked) = v
        val indices = generateMarked(total, marked)
        _uiState.value = _uiState.value.copy(markedIndices = indices)
    }

    fun hideOverlay() {
        _uiState.value = _uiState.value.copy(isOverlayVisible = false)
    }
}
