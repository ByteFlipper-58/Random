package com.byteflipper.random.ui.finger

import android.app.Activity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.ads.AdsController
import com.byteflipper.random.data.settings.FINGER_HOLD_DURATION_MAX_MS
import com.byteflipper.random.data.settings.FINGER_HOLD_DURATION_MIN_MS
import com.byteflipper.random.data.settings.FINGER_RESULT_HOLD_SECONDS_MAX
import com.byteflipper.random.data.settings.FINGER_RESULT_HOLD_SECONDS_MIN
import com.byteflipper.random.data.settings.FINGER_TEAM_COUNT_MAX
import com.byteflipper.random.data.settings.FINGER_TEAM_COUNT_MIN
import com.byteflipper.random.data.settings.FINGER_WINNER_COUNT_MAX
import com.byteflipper.random.data.settings.FINGER_WINNER_COUNT_MIN
import com.byteflipper.random.data.settings.HapticsIntensity
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.finger.usecase.GenerateFingerOutcomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FingerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val generateFingerOutcomeUseCase: GenerateFingerOutcomeUseCase,
    private val adsController: AdsController
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val _uiState = MutableStateFlow(FingerUiState())
    val uiState: StateFlow<FingerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FingerUiEffect>(Channel.BUFFERED)
    val effects: Flow<FingerUiEffect> = _effects.receiveAsFlow()

    private var countdownJob: Job? = null
    private var resultShowcaseJob: Job? = null
    private val assignedColors = mutableMapOf<Long, Color>()
    private var latestPointers = listOf<Pair<Long, Offset>>()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map {
                    FingerSettingsData(
                        winnerCount = it.fingerWinnerCount,
                        teamCount = it.fingerTeamCount,
                        holdDurationMs = it.fingerHoldDurationMs,
                        holdResultEnabled = it.fingerHoldResultEnabled,
                        resultHoldDurationSeconds = it.fingerResultHoldDurationSeconds
                    )
                }
                .distinctUntilChanged()
                .collect { data ->
                    _uiState.update { current ->
                        current.copy(
                            winnerCount = data.winnerCount,
                            teamCount = data.teamCount,
                            holdDurationMs = data.holdDurationMs,
                            holdResultEnabled = data.holdResultEnabled,
                            resultHoldDurationSeconds = data.resultHoldDurationSeconds
                        )
                    }
                }
        }
    }

    fun onEvent(event: FingerUiEvent) {
        when (event) {
            is FingerUiEvent.PointersChanged -> handlePointersChanged(event.pointers)
            is FingerUiEvent.SetMode -> {
                if (_uiState.value.phase != FingerPhase.DECIDED) {
                    cancelCountdown()
                    _uiState.update { it.copy(mode = event.mode, progress = 0f, phase = FingerPhase.IDLE) }
                    recheckState()
                } else {
                    _uiState.update { it.copy(mode = event.mode) }
                }
            }
            is FingerUiEvent.SetWinnerCount -> {
                val clamped = event.count.coerceIn(FINGER_WINNER_COUNT_MIN, FINGER_WINNER_COUNT_MAX)
                _uiState.update { it.copy(winnerCount = clamped) }
                viewModelScope.launch { settingsRepository.setFingerWinnerCount(clamped) }
                recheckState()
            }
            is FingerUiEvent.SetTeamCount -> {
                val clamped = event.count.coerceIn(FINGER_TEAM_COUNT_MIN, FINGER_TEAM_COUNT_MAX)
                _uiState.update { it.copy(teamCount = clamped) }
                viewModelScope.launch { settingsRepository.setFingerTeamCount(clamped) }
                recheckState()
            }
            is FingerUiEvent.SetHoldDuration -> {
                val clamped = event.durationMs.coerceIn(FINGER_HOLD_DURATION_MIN_MS, FINGER_HOLD_DURATION_MAX_MS)
                _uiState.update { it.copy(holdDurationMs = clamped) }
                viewModelScope.launch { settingsRepository.setFingerHoldDurationMs(clamped) }
            }
            is FingerUiEvent.SetHoldResultEnabled -> {
                _uiState.update { it.copy(holdResultEnabled = event.enabled) }
                viewModelScope.launch { settingsRepository.setFingerHoldResultEnabled(event.enabled) }
            }
            is FingerUiEvent.SetResultHoldDurationSeconds -> {
                val clamped = event.seconds.coerceIn(FINGER_RESULT_HOLD_SECONDS_MIN, FINGER_RESULT_HOLD_SECONDS_MAX)
                _uiState.update { it.copy(resultHoldDurationSeconds = clamped) }
                viewModelScope.launch { settingsRepository.setFingerResultHoldDurationSeconds(clamped) }
            }
            is FingerUiEvent.ToggleSettingsSheet -> {
                _uiState.update { it.copy(showSettingsSheet = event.visible) }
            }
            FingerUiEvent.Reset -> resetAll()
        }
    }

    fun checkAd(activity: Activity) {
        adsController.onFingerOutcome(activity)
    }

    private fun handlePointersChanged(pointers: List<Pair<Long, Offset>>) {
        latestPointers = pointers
        val currentPhase = _uiState.value.phase
        val pointerIds = pointers.map { it.first }.toSet()

        if (currentPhase == FingerPhase.DECIDED) {
            if (_uiState.value.isResultLocked && _uiState.value.holdResultEnabled) {
                // Result is showcase-locked on screen — keep outcome displayed
                return
            }
            // If lock disabled or time elapsed, reset when all fingers are lifted
            if (pointers.isEmpty()) {
                resetAll()
            }
            return
        }

        // Clean up unneeded color mappings
        assignedColors.keys.retainAll(pointerIds)

        if (pointers.isEmpty()) {
            cancelCountdown()
            assignedColors.clear()
            _uiState.update {
                it.copy(
                    phase = FingerPhase.IDLE,
                    progress = 0f,
                    activeFingers = emptyMap()
                )
            }
            return
        }

        // Map pointers to TouchPoint
        val newFingers = mutableMapOf<Long, TouchPoint>()
        pointers.forEachIndexed { index, (id, pos) ->
            val color = assignedColors.getOrPut(id) {
                val availableColors = FINGER_PALETTE.filter { it !in assignedColors.values }
                if (availableColors.isNotEmpty()) {
                    availableColors.first()
                } else {
                    FINGER_PALETTE[index % FINGER_PALETTE.size]
                }
            }
            newFingers[id] = TouchPoint(
                id = id,
                position = pos,
                color = color
            )
        }

        val previousFingerCount = _uiState.value.fingerCount
        val minNeeded = _uiState.value.minFingersNeeded

        _uiState.update {
            it.copy(activeFingers = newFingers)
        }

        if (newFingers.size >= minNeeded) {
            // Restart countdown if fingers count changed or not running
            if (countdownJob?.isActive != true || newFingers.size != previousFingerCount) {
                startCountdown()
            }
        } else {
            cancelCountdown()
            _uiState.update { it.copy(phase = FingerPhase.IDLE, progress = 0f) }
        }
    }

    private fun recheckState() {
        val state = _uiState.value
        if (state.phase == FingerPhase.DECIDED) return

        if (state.activeFingers.size >= state.minFingersNeeded) {
            startCountdown()
        } else {
            cancelCountdown()
            _uiState.update { it.copy(phase = FingerPhase.IDLE, progress = 0f) }
        }
    }

    private fun startCountdown() {
        cancelCountdown()
        val duration = _uiState.value.holdDurationMs

        countdownJob = viewModelScope.launch {
            _uiState.update { it.copy(phase = FingerPhase.HOLDING, progress = 0f) }

            val interval = 20L
            val totalSteps = (duration / interval).toInt().coerceAtLeast(1)
            var lastPulseStep = 0

            for (step in 1..totalSteps) {
                delay(interval)
                val progress = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
                _uiState.update { it.copy(progress = progress) }

                // Periodic pulses getting faster as we near completion
                val pulseInterval = when {
                    progress > 0.8f -> (totalSteps * 0.08f).toInt().coerceAtLeast(2)
                    progress > 0.5f -> (totalSteps * 0.15f).toInt().coerceAtLeast(4)
                    else -> (totalSteps * 0.30f).toInt().coerceAtLeast(8)
                }

                if (step - lastPulseStep >= pulseInterval && step < totalSteps) {
                    lastPulseStep = step
                    val intensity = if (progress > 0.7f) HapticsIntensity.Medium else HapticsIntensity.Low
                    _effects.send(FingerUiEffect.HapticPulse(intensity))
                }
            }

            decideOutcome()
        }
    }

    private suspend fun decideOutcome() {
        val state = _uiState.value
        val fingers = state.activeFingers.values.toList()
        if (fingers.size < state.minFingersNeeded) {
            _uiState.update { it.copy(phase = FingerPhase.IDLE, progress = 0f) }
            return
        }

        val outcomes = generateFingerOutcomeUseCase(
            GenerateFingerOutcomeUseCase.Params(
                participantIds = fingers.map { it.id },
                mode = state.mode,
                winnerCount = state.winnerCount,
                teamCount = state.teamCount
            )
        ).associateBy { it.id }

        val updatedMap = mutableMapOf<Long, TouchPoint>()
        fingers.forEach { touch ->
            val outcome = outcomes[touch.id]
            val teamColor = outcome?.teamIndex?.let { TEAM_COLORS[(it - 1) % TEAM_COLORS.size] }
            updatedMap[touch.id] = touch.copy(
                isWinner = outcome?.isWinner ?: false,
                teamIndex = outcome?.teamIndex,
                teamColor = teamColor,
                orderIndex = outcome?.orderIndex
            )
        }

        val holdSec = state.resultHoldDurationSeconds
        val shouldLock = state.holdResultEnabled

        _uiState.update {
            it.copy(
                phase = FingerPhase.DECIDED,
                progress = 1f,
                activeFingers = updatedMap,
                isResultLocked = shouldLock,
                resultRemainingSeconds = holdSec
            )
        }

        _effects.send(FingerUiEffect.HapticWinner())
        _effects.send(FingerUiEffect.TriggerConfetti)

        // Result showcase timer
        resultShowcaseJob?.cancel()
        if (shouldLock) {
            resultShowcaseJob = viewModelScope.launch {
                for (sec in holdSec downTo 1) {
                    _uiState.update { it.copy(resultRemainingSeconds = sec) }
                    delay(1000L)
                }
                _uiState.update { it.copy(isResultLocked = false) }
                if (latestPointers.isEmpty()) {
                    resetAll()
                }
            }
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun resetAll() {
        cancelCountdown()
        resultShowcaseJob?.cancel()
        resultShowcaseJob = null
        assignedColors.clear()
        _uiState.update {
            it.copy(
                phase = FingerPhase.IDLE,
                progress = 0f,
                isResultLocked = false,
                resultRemainingSeconds = it.resultHoldDurationSeconds,
                activeFingers = emptyMap()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelCountdown()
        resultShowcaseJob?.cancel()
    }
}

private data class FingerSettingsData(
    val winnerCount: Int,
    val teamCount: Int,
    val holdDurationMs: Long,
    val holdResultEnabled: Boolean,
    val resultHoldDurationSeconds: Int
)
