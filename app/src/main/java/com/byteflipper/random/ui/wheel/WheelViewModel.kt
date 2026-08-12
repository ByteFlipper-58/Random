package com.byteflipper.random.ui.wheel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.WHEEL_SPIN_DURATION_DEFAULT_MS
import com.byteflipper.random.data.settings.WHEEL_SPIN_DURATION_MAX_MS
import com.byteflipper.random.data.settings.WHEEL_SPIN_DURATION_MIN_MS
import com.byteflipper.random.data.settings.WheelUsedSectorStyle
import com.byteflipper.random.ui.common.defaultRandomItems
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class WheelUiState(
    val items: List<String> = emptyList(),
    val excludedIndices: Set<Int> = emptySet(),
    val noRepeats: Boolean = false,
    val usedSectorStyle: WheelUsedSectorStyle = WheelUsedSectorStyle.Dim,
    val spinDuration: Int = WHEEL_SPIN_DURATION_DEFAULT_MS,
    val lastResult: String? = null,
    val lastResultIndex: Int? = null,
    val isSpinning: Boolean = false,
    /**
     * The final round has been played: the winner was picked out of the last [WHEEL_MIN_ITEMS]
     * sectors and is not excluded. There is nothing left to spin without a reset.
     */
    val needsReset: Boolean = false,
    /** Current wheel rotation, kept here to survive the composable being recreated. */
    val rotation: Float = 0f,
    val showEditorSheet: Boolean = false,
    val showSettingsSheet: Boolean = false
)

/** Result of [WheelViewModel.spin]: the winner is known up front, the angle only leads to it. */
data class WheelSpin(
    val winnerIndex: Int,
    val targetRotation: Float
)

@HiltViewModel
class WheelViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
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

    private val _uiState = MutableStateFlow(WheelUiState(items = appContext.defaultRandomItems()))
    val uiState: StateFlow<WheelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map {
                    Triple(it.wheelNoRepeats, it.wheelSpinDurationMs, it.wheelUsedSectorStyle)
                }
                .distinctUntilChanged()
                .collect { (noRepeats, durationMs, usedSectorStyle) ->
                    _uiState.update {
                        it.copy(
                            noRepeats = noRepeats,
                            usedSectorStyle = usedSectorStyle,
                            spinDuration = durationMs.coerceIn(
                                WHEEL_SPIN_DURATION_MIN_MS,
                                WHEEL_SPIN_DURATION_MAX_MS
                            )
                        )
                    }
                }
        }
    }

    fun onEvent(event: WheelUiEvent) {
        when (event) {
            is WheelUiEvent.UpdateItems -> updateItems(event.items)
            is WheelUiEvent.SetNoRepeats -> setNoRepeats(event.enabled)
            is WheelUiEvent.SetUsedSectorStyle -> setUsedSectorStyle(event.style)
            is WheelUiEvent.SetSpinDuration -> setSpinDuration(event.durationMs)
            is WheelUiEvent.LoadPreset -> loadPreset(event.preset)
            is WheelUiEvent.CommitSpin -> commitSpin(event.winnerIndex, event.finalRotation)
            is WheelUiEvent.CancelSpin -> cancelSpin(event.currentRotation)
            WheelUiEvent.Reset -> reset()
            WheelUiEvent.ToggleEditorSheet -> _uiState.update { it.copy(showEditorSheet = !it.showEditorSheet) }
            WheelUiEvent.ToggleSettingsSheet -> _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
        }
    }

    private fun updateItems(items: List<String>) {
        val currentItems = _uiState.value.items
        // Only reset excluded if items actually changed
        if (items != currentItems) {
            _uiState.update {
                it.copy(
                    items = items,
                    excludedIndices = emptySet(),
                    lastResult = null,
                    lastResultIndex = null,
                    needsReset = false
                )
            }
        }
    }

    private fun setNoRepeats(enabled: Boolean) {
        // Turning "no repeats" off clears the final round as well, otherwise the wheel would stay
        // blocked for no visible reason.
        _uiState.update { it.copy(noRepeats = enabled, needsReset = it.needsReset && enabled) }
        viewModelScope.launch { settingsRepository.setWheelNoRepeats(enabled) }
    }

    private fun setUsedSectorStyle(style: WheelUsedSectorStyle) {
        _uiState.update { it.copy(usedSectorStyle = style) }
        viewModelScope.launch { settingsRepository.setWheelUsedSectorStyle(style) }
    }

    private fun setSpinDuration(durationMs: Int) {
        val coerced = durationMs.coerceIn(WHEEL_SPIN_DURATION_MIN_MS, WHEEL_SPIN_DURATION_MAX_MS)
        _uiState.update { it.copy(spinDuration = coerced) }
        viewModelScope.launch { settingsRepository.setWheelSpinDurationMs(coerced) }
    }

    private fun loadPreset(preset: ListPreset) {
        _uiState.update {
            it.copy(
                items = preset.items,
                excludedIndices = emptySet(),
                lastResult = null,
                lastResultIndex = null,
                needsReset = false
            )
        }
        // A synthetic preset built from people has id = 0 and no row in the table.
        if (preset.id != 0L) {
            viewModelScope.launch {
                listPresetRepository.markUsed(preset.id)
            }
        }
    }

    /**
     * Picks the winner and the angle that puts it under the pointer.
     *
     * [currentRotation] is the actual animated angle, so the target is always ahead of it.
     * [fullTurns] and [clockwise] only shape the path and come from the gesture velocity on a
     * fling; the winner is still chosen here and up front.
     */
    fun spin(
        currentRotation: Float,
        fullTurns: Int,
        clockwise: Boolean = true
    ): WheelSpin? {
        val state = _uiState.value
        if (state.isSpinning || state.needsReset) return null

        val visibleIndices = state.items.indices.filter { it !in state.excludedIndices }
        if (visibleIndices.isEmpty()) return null

        val winnerIndex = visibleIndices[Random.nextInt(visibleIndices.size)]

        // Where the winner sits depends on the used sector style, so ask for the same layout the
        // wheel is drawn from.
        val sectors = wheelSectors(
            items = state.items,
            excludedIndices = state.excludedIndices,
            usedSectorStyle = state.usedSectorStyle
        )
        val sectorPosition = sectors.indexOfFirst { it.index == winnerIndex }
        if (sectorPosition < 0) return null

        // Stop within 10..90% of the sector to stay clear of rounding at its edges.
        val sectorFraction = 0.1f + Random.nextFloat() * 0.8f
        val rotationInTurn = WheelGeometry.rotationForSector(
            sectorIndex = sectorPosition,
            itemCount = sectors.size,
            sectorFraction = sectorFraction
        )
        val targetRotation = WheelGeometry.animationTarget(
            from = currentRotation,
            fullTurns = fullTurns,
            targetRotationInTurn = rotationInTurn,
            clockwise = clockwise
        )

        _uiState.update { it.copy(isSpinning = true, rotation = currentRotation) }

        return WheelSpin(winnerIndex = winnerIndex, targetRotation = targetRotation)
    }

    /**
     * Commits the result by the index chosen in [spin].
     * By index and not by value: duplicate items would otherwise exclude the wrong sector.
     */
    private fun commitSpin(winnerIndex: Int, finalRotation: Float) {
        val state = _uiState.value
        val result = state.items.getOrNull(winnerIndex)

        if (result == null) {
            _uiState.update { it.copy(isSpinning = false, rotation = finalRotation) }
            return
        }

        // The wheel must not be scraped down to a single sector: once excluding would leave fewer
        // than the minimum, the winner is announced but stays on the wheel. That is the final round.
        val visibleCount = state.items.size - state.excludedIndices.size
        val isFinalRound = state.noRepeats && visibleCount - 1 < WHEEL_MIN_ITEMS

        val newExcluded = if (state.noRepeats && !isFinalRound) {
            state.excludedIndices + winnerIndex
        } else {
            state.excludedIndices
        }

        _uiState.update {
            it.copy(
                lastResult = result,
                lastResultIndex = winnerIndex,
                excludedIndices = newExcluded,
                needsReset = isFinalRound,
                isSpinning = false,
                rotation = finalRotation
            )
        }
    }

    /** The animation was cut short (leaving the screen, configuration change): never stay stuck. */
    private fun cancelSpin(currentRotation: Float) {
        _uiState.update { it.copy(isSpinning = false, rotation = currentRotation) }
    }

    private fun reset() {
        _uiState.update {
            it.copy(
                excludedIndices = emptySet(),
                lastResult = null,
                lastResultIndex = null,
                needsReset = false
            )
        }
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
    data class SetUsedSectorStyle(val style: WheelUsedSectorStyle) : WheelUiEvent
    data class SetSpinDuration(val durationMs: Int) : WheelUiEvent
    data class LoadPreset(val preset: ListPreset) : WheelUiEvent
    data class CommitSpin(val winnerIndex: Int, val finalRotation: Float) : WheelUiEvent
    data class CancelSpin(val currentRotation: Float) : WheelUiEvent
    data object Reset : WheelUiEvent
    data object ToggleEditorSheet : WheelUiEvent
    data object ToggleSettingsSheet : WheelUiEvent
}
