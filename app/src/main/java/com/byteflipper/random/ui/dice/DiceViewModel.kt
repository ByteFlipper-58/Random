package com.byteflipper.random.ui.dice

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.ads.AdsController
import com.byteflipper.random.data.settings.SimulationQuality
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.physics.SimulationQualityTier
import com.byteflipper.random.domain.physics.Vec3
import com.byteflipper.random.domain.dice.physics.DiceCommand
import com.byteflipper.random.domain.dice.physics.DiceEngine
import com.byteflipper.random.domain.dice.physics.DiceEngineEvent
import com.byteflipper.random.domain.dice.physics.DiceEngineTuning
import com.byteflipper.random.domain.dice.physics.DiceImpactMaterial
import com.byteflipper.random.domain.dice.physics.DiceTossKind
import com.byteflipper.random.domain.dice.usecase.RollDiceUseCase
import com.byteflipper.random.utils.DeviceTossGesture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiceUiState(
    val diceCount: Int = 2,
    val values: List<Int> = listOf(1, 2),
    val isOverlayVisible: Boolean = false,
    val showSettingsSheet: Boolean = false,
    /**
     * Which dice the screen is showing: the flat ones, or the tray. Null until the stored choice has
     * come back out of settings, because the two look nothing alike — drawing either one on a guess
     * means a flash of the wrong screen on every entry for whoever guessed wrong.
     */
    val threeDimensional: Boolean? = null,
    /** The app-wide tier, mirrored here only to tell the renderer whether Auto is in charge. */
    val quality: SimulationQuality = SimulationQuality.Auto,
    /** True from a throw until the last cube stops. Only the tray has a roll that lasts. */
    val isTrayRolling: Boolean = false
)

@HiltViewModel
class DiceViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val rollDice: RollDiceUseCase,
    private val adsController: AdsController
) : ViewModel() {

    /**
     * The tray simulation, for when the dice are the 3D ones.
     *
     * It lives here rather than in the renderer so a throw carries on across a surface recreation, and
     * so measured phone motion can reach it without a GL context. Nothing moves until something steps it, so
     * the flat dice pay only for the objects.
     */
    val engine = DiceEngine(DiceEngineTuning.forTier(SimulationQualityTier.BALANCED))

    val settings: StateFlow<Settings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val _uiState = MutableStateFlow(DiceUiState())
    val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

    /**
     * Landings and finished throws.
     *
     * Dropped rather than queued when nothing is collecting: these are all things that only mean
     * anything as they happen, and a haptic tick for a landing from a minute ago would arrive as a
     * buzz out of nowhere.
     */
    private val _effects = MutableSharedFlow<DiceUiEffect>(
        extraBufferCapacity = EFFECT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effects: SharedFlow<DiceUiEffect> = _effects

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.dice3dEnabled }
                .distinctUntilChanged()
                .collect { threeDimensional ->
                    _uiState.update {
                        it.copy(threeDimensional = threeDimensional, isTrayRolling = false)
                    }
                    // Whether it was switched on just now or was on when the screen opened, a tray
                    // coming into view has never been told how many dice it holds.
                    if (threeDimensional) engine.submit(DiceCommand.Arrange(_uiState.value.values))
                }
        }

        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { it.graphicsQuality }
                .distinctUntilChanged()
                .collect { quality ->
                    _uiState.update { it.copy(quality = quality) }
                    engine.setTuning(DiceEngineTuning.forTier(quality.toTier()))
                }
        }

        viewModelScope.launch {
            engine.events.collect { event ->
                when (event) {
                    is DiceEngineEvent.Impact -> _effects.tryEmit(
                        DiceUiEffect.Impact(event.strength, event.material)
                    )
                    is DiceEngineEvent.Settled -> onTraySettled(event.values)
                }
            }
        }
    }

    fun onEvent(event: DiceUiEvent) {
        when (event) {
            is DiceUiEvent.SetDiceCount -> setDiceCount(event.count)
            is DiceUiEvent.SetOverlayVisible -> setOverlayVisible(event.visible)
            is DiceUiEvent.RollAll -> rollAll()
            is DiceUiEvent.RollOne -> rollOne(event.index)
            is DiceUiEvent.RollTray -> rollTray()
            is DiceUiEvent.ToggleSettingsSheet ->
                _uiState.update { it.copy(showSettingsSheet = event.visible) }
            is DiceUiEvent.SetThreeDimensional -> setThreeDimensional(event.enabled)
            is DiceUiEvent.SetShakeToRoll -> setShakeToRoll(event.enabled)
        }
    }

    fun setDiceCount(count: Int) {
        val clamped = count.coerceIn(1, DiceEngineTuning.MAX_DICE)
        val base = _uiState.value.values.take(clamped)
        val padded = base + List(clamped - base.size) { 1 }
        _uiState.update { it.copy(diceCount = clamped, values = padded, isTrayRolling = false) }
        // A different number of dice is a different tray: they are laid out again, and bigger or
        // smaller for there being fewer or more of them.
        if (_uiState.value.threeDimensional == true) engine.submit(DiceCommand.Arrange(padded))
    }

    fun setOverlayVisible(visible: Boolean) {
        _uiState.update { it.copy(isOverlayVisible = visible) }
    }

    fun rollAll(): List<Int> {
        val count = _uiState.value.diceCount
        val newValues = rollDice(count)
        _uiState.update { it.copy(values = newValues) }
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
        _uiState.update { it.copy(values = newList) }
        return value
    }

    /**
     * Throws the tray's dice.
     *
     * Unlike the flat animation, the 3D result comes from the upward physical faces after every body
     * has gone to sleep. Until then the previous result remains in UI state.
     */
    private fun rollTray() {
        if (_uiState.value.isTrayRolling) return
        _uiState.update { it.copy(isTrayRolling = true) }
        engine.submit(DiceCommand.Roll(_uiState.value.diceCount))
    }

    private fun onTraySettled(values: List<Int>) {
        // A count change can overtake the previous tray's completion event on the GL/UI boundary.
        if (values.size != _uiState.value.diceCount) return
        val wasThrow = _uiState.value.isTrayRolling
        _uiState.update { it.copy(values = values, isTrayRolling = false) }
        if (wasThrow) _effects.tryEmit(DiceUiEffect.Rolled(values))
    }

    /** How hard the phone itself is being moved, in g. */
    fun onMotion(acceleration: Vec3) {
        if (_uiState.value.threeDimensional != true) return
        engine.submit(DiceCommand.Motion(acceleration))
    }

    /** A deliberate phone throw, with strength measured from the real gesture. */
    fun onTossGesture(gesture: DeviceTossGesture, strength: Float) {
        if (_uiState.value.threeDimensional != true) return
        if (!settings.value.shakeToGenerateEnabled) return
        if (_uiState.value.isTrayRolling) return
        _uiState.update { it.copy(isTrayRolling = true) }
        val kind = when (gesture) {
            DeviceTossGesture.TABLE_PITCH -> DiceTossKind.TABLE_PITCH
            DeviceTossGesture.VERTICAL_LIFT -> DiceTossKind.VERTICAL_LIFT
        }
        engine.submit(DiceCommand.Toss(kind, strength))
    }

    /**
     * Switches the tray on or off.
     *
     * The state is moved before the write lands so the switch and the dice under it turn over
     * together; the settings collector then arrives at the same value and arranges the tray.
     */
    private fun setThreeDimensional(enabled: Boolean) {
        _uiState.update { it.copy(threeDimensional = enabled, isTrayRolling = false) }
        viewModelScope.launch { settingsRepository.setDice3dEnabled(enabled) }
    }

    /** App-wide, and read straight back from settings — there is nothing dice-specific to mirror. */
    private fun setShakeToRoll(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShakeToGenerateEnabled(enabled) }
    }

    private fun SimulationQuality.toTier(): SimulationQualityTier = when (this) {
        SimulationQuality.High -> SimulationQualityTier.HIGH
        SimulationQuality.Balanced -> SimulationQualityTier.BALANCED
        SimulationQuality.Battery -> SimulationQualityTier.BATTERY
        // Auto starts here and the renderer's frame meter moves it once it has a reading.
        SimulationQuality.Auto -> SimulationQualityTier.BALANCED
    }

    fun checkAd(activity: Activity) {
        adsController.onDiceRolled(activity)
    }
}

sealed interface DiceUiEvent {
    data class SetDiceCount(val count: Int) : DiceUiEvent
    data class SetOverlayVisible(val visible: Boolean) : DiceUiEvent
    data object RollAll : DiceUiEvent
    data class RollOne(val index: Int) : DiceUiEvent

    /** Throw the 3D dice. The flat ones roll through the screen's own animation instead. */
    data object RollTray : DiceUiEvent

    data class ToggleSettingsSheet(val visible: Boolean) : DiceUiEvent

    /** Swap the flat dice for the tray, or back. */
    data class SetThreeDimensional(val enabled: Boolean) : DiceUiEvent

    data class SetShakeToRoll(val enabled: Boolean) : DiceUiEvent
}

sealed interface DiceUiEffect {

    /** A cube landed; presentation decides independently whether to play sound or haptics. */
    data class Impact(
        val strength: Float,
        val material: DiceImpactMaterial
    ) : DiceUiEffect

    /** Every cube has stopped, showing [values]. */
    data class Rolled(val values: List<Int>) : DiceUiEffect
}

/** Enough to hold a handful of dice landing at once without dropping the last of them. */
private const val EFFECT_BUFFER = 16
