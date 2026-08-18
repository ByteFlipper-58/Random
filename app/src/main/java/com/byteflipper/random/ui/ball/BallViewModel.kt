package com.byteflipper.random.ui.ball

import android.app.Activity
import com.byteflipper.random.ads.AdsController
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.SimulationQuality
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.ball.data.BallAnswerProvider
import com.byteflipper.random.domain.ball.model.BallAnswerSource
import com.byteflipper.random.domain.ball.physics.BallCommand
import com.byteflipper.random.domain.ball.physics.BallEngine
import com.byteflipper.random.domain.ball.physics.BallEngineEvent
import com.byteflipper.random.domain.ball.physics.BallEngineTuning
import com.byteflipper.random.domain.physics.SimulationQualityTier
import com.byteflipper.random.domain.physics.Vec3
import com.byteflipper.random.domain.ball.usecase.AskBallUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How hard a detected shake hits the simulation, in multiples of g above the threshold. */
private const val SHAKE_MAGNITUDE = 1.8f

@HiltViewModel
class BallViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val listPresetRepository: ListPresetRepository,
    private val ballAnswerProvider: BallAnswerProvider,
    private val askBallUseCase: AskBallUseCase,
    private val adsController: AdsController
) : ViewModel() {

    /**
     * The simulation. It lives here rather than in the renderer so that the liquid keeps sloshing
     * across a surface recreation, and so tilt and shake can reach it without a GL context.
     */
    val engine = BallEngine(BallEngineTuning.forTier(SimulationQualityTier.BALANCED))

    val settings: StateFlow<Settings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val _uiState = MutableStateFlow(BallUiState())
    val uiState: StateFlow<BallUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BallUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Only the fields the ball cares about, so a haptics change does not re-read the presets. */
    private data class BallSettings(
        val sourceId: Long,
        val customAnswers: List<String>,
        val noRepeats: Boolean,
        val tiltEnabled: Boolean,
        val quality: SimulationQuality
    )

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map {
                    BallSettings(
                        sourceId = it.ballAnswerSourceId,
                        customAnswers = it.ballCustomAnswers,
                        noRepeats = it.ballNoRepeats,
                        tiltEnabled = it.ballTiltEnabled,
                        quality = it.graphicsQuality
                    )
                }
                .distinctUntilChanged()
                .collectLatest { applySettings(it) }
        }

        viewModelScope.launch {
            engine.events.collect { event ->
                when (event) {
                    is BallEngineEvent.Impact -> _effects.send(BallUiEffect.Impact(event.strength))
                    BallEngineEvent.Revealed -> reveal()
                }
            }
        }
    }

    private suspend fun applySettings(ballSettings: BallSettings) {
        val source = BallAnswerSource.fromSettings(
            sourceId = ballSettings.sourceId,
            customAnswers = ballSettings.customAnswers
        )
        val presetItems = (source as? BallAnswerSource.Preset)
            ?.let { listPresetRepository.getById(it.id)?.items }
        val answers = ballAnswerProvider.resolve(source, presetItems)

        engine.setTuning(BallEngineTuning.forTier(ballSettings.quality.toTier()))

        val previous = _uiState.value
        if (previous.answers != answers && previous.phase != BallPhase.IDLE) {
            // A different set invalidates the ask in flight, so calm the ball down as well.
            engine.submit(BallCommand.Reset)
        }

        _uiState.update { state ->
            // A different set invalidates the index the previous ask produced.
            val answersChanged = state.answers != answers
            state.copy(
                answers = answers,
                source = source,
                noRepeats = ballSettings.noRepeats,
                tiltEnabled = ballSettings.tiltEnabled,
                quality = ballSettings.quality,
                answerIndex = if (answersChanged) null else state.answerIndex,
                phase = if (answersChanged) BallPhase.IDLE else state.phase
            )
        }
    }

    fun onEvent(event: BallUiEvent) {
        when (event) {
            BallUiEvent.Ask -> ask()
            BallUiEvent.Reset -> reset()
            is BallUiEvent.SetNoRepeats -> setNoRepeats(event.enabled)
            is BallUiEvent.SetTiltEnabled -> setTiltEnabled(event.enabled)
            is BallUiEvent.ToggleSettingsSheet ->
                _uiState.update { it.copy(showSettingsSheet = event.visible) }
        }
    }

    /**
     * Draws the answer and starts the reveal.
     *
     * The index is decided here and now, before anything moves: the animation only has to bring
     * that face to the window, the way the wheel already works.
     */
    private fun ask() {
        val state = _uiState.value
        if (!state.canAsk) return

        val index = askBallUseCase(
            AskBallUseCase.Params(
                answerCount = state.answers.size,
                previousIndex = state.answerIndex,
                avoidRepeat = state.noRepeats
            )
        ) ?: return

        _uiState.update { it.copy(phase = BallPhase.ASKING, answerIndex = index) }
        // The face the answer is printed on; the atlas follows the same rule from the UI state.
        engine.submit(BallCommand.Ask(faceIndexFor(index)))

        viewModelScope.launch { _effects.send(BallUiEffect.HapticPulse) }
    }

    /**
     * Announces the answer, once.
     *
     * The engine says when: the die has to have come to rest against the window before there is
     * anything to read. A reset in the meantime moves the phase on, and this then does nothing.
     */
    private suspend fun reveal() {
        if (_uiState.value.phase != BallPhase.ASKING) return

        _uiState.update { it.copy(phase = BallPhase.REVEALED) }
        val answer = _uiState.value.answer ?: return
        _effects.send(BallUiEffect.AnswerRevealed(answer))
    }

    /** Reports device gravity in view space; ignored unless the player asked for tilt. */
    fun onTilt(gravity: Vec3) {
        if (!_uiState.value.tiltEnabled) return
        engine.submit(BallCommand.Tilt(gravity))
    }

    /**
     * Reports how hard the phone itself is being moved, in g.
     *
     * Never gated by the tilt setting: that switch is about the ball following which way is down,
     * whereas reacting to a shake is the toy itself.
     */
    fun onMotion(acceleration: Vec3) {
        engine.submit(BallCommand.Motion(acceleration))
    }

    /** A shake both stirs the liquid and counts as an ask, the way a real 8-ball works. */
    fun onShake() {
        engine.submit(BallCommand.Shake(SHAKE_MAGNITUDE))
        ask()
    }

    private fun reset() {
        engine.submit(BallCommand.Reset)
        _uiState.update { it.copy(phase = BallPhase.IDLE, answerIndex = null) }
    }

    private fun SimulationQuality.toTier(): SimulationQualityTier = when (this) {
        SimulationQuality.High -> SimulationQualityTier.HIGH
        SimulationQuality.Balanced -> SimulationQualityTier.BALANCED
        SimulationQuality.Battery -> SimulationQualityTier.BATTERY
        // Auto starts here and the renderer's frame meter moves it from this tier once it has a
        // reading; the middle one is both the safe default and a fair thing to measure against.
        SimulationQuality.Auto -> SimulationQualityTier.BALANCED
    }

    private fun setNoRepeats(enabled: Boolean) {
        _uiState.update { it.copy(noRepeats = enabled) }
        viewModelScope.launch { settingsRepository.setBallNoRepeats(enabled) }
    }

    private fun setTiltEnabled(enabled: Boolean) {
        _uiState.update { it.copy(tiltEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setBallTiltEnabled(enabled) }
    }

    fun checkAd(activity: Activity) {
        adsController.onBallAnswered(activity)
    }
}
