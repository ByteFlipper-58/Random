package com.byteflipper.random.ui.ball

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.ball.data.BallAnswerProvider
import com.byteflipper.random.domain.ball.model.BallAnswerSource
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

/**
 * The answers editor.
 *
 * It owns nothing the ball needs at run time: picking a source and saving a list both go into
 * settings, and the ball's own view model is already listening there. That is why this screen can be
 * opened and left without handing anything back through navigation.
 */
@HiltViewModel
class BallAnswersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val listPresetRepository: ListPresetRepository,
    private val ballAnswerProvider: BallAnswerProvider
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    /** Saved lists that can become the ball's source, newest first as the repository orders them. */
    val presets: StateFlow<List<ListPreset>> = listPresetRepository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(BallAnswersUiState())
    val uiState: StateFlow<BallAnswersUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BallAnswersUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Row ids only have to be unique within this editor's lifetime. */
    private var nextDraftId = 0L

    /** The two settings fields the editor works from; anything else must not reseed the draft. */
    private data class StoredSource(
        val sourceId: Long,
        val customAnswers: List<String>
    )

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow
                .map { StoredSource(it.ballAnswerSourceId, it.ballCustomAnswers) }
                .distinctUntilChanged()
                .collectLatest { stored -> applyStored(stored) }
        }
    }

    /**
     * Seeds the editor from whatever the ball is following, and reseeds it whenever that changes —
     * which is how picking the classic set or a preset shows up in the list below the chips.
     */
    private suspend fun applyStored(stored: StoredSource) {
        val source = BallAnswerSource.fromSettings(
            sourceId = stored.sourceId,
            customAnswers = stored.customAnswers
        )
        val presetItems = (source as? BallAnswerSource.Preset)
            ?.let { listPresetRepository.getById(it.id)?.items }
        val saved = ballAnswerProvider.resolve(source, presetItems).map { it.text }
        val draft = saved.map { text -> BallAnswerDraft(nextDraftId++, text) }

        _uiState.update { state ->
            state.copy(source = source, saved = saved, draft = draft)
        }
    }

    fun updateAnswer(id: Long, text: String) {
        if (text.length > BALL_MAX_ANSWER_LENGTH) return
        _uiState.update { state ->
            state.copy(
                draft = state.draft.map { row -> if (row.id == id) row.copy(text = text) else row }
            )
        }
    }

    /** Appends what the field at the top holds. Returns false when there was nothing to add. */
    fun addAnswer(text: String): Boolean {
        val trimmed = text.trim().take(BALL_MAX_ANSWER_LENGTH)
        if (trimmed.isEmpty()) return false

        if (_uiState.value.atLimit) {
            notify(R.string.ball_answers_limit, BALL_MAX_ANSWERS)
            return false
        }

        val row = BallAnswerDraft(nextDraftId++, trimmed)
        _uiState.update { state -> state.copy(draft = state.draft + row) }
        return true
    }

    fun removeAnswer(id: Long) {
        _uiState.update { state ->
            state.copy(draft = state.draft.filterNot { row -> row.id == id })
        }
    }

    /**
     * Puts the row [fromId] where [toId] currently is, pushing the rest along.
     *
     * By id rather than by position, because a drag is a stream of small moves and the position a
     * gesture started from stops being true after the first one.
     */
    fun moveAnswer(fromId: Long, toId: Long) {
        _uiState.update { state ->
            val from = state.draft.indexOfFirst { row -> row.id == fromId }
            val to = state.draft.indexOfFirst { row -> row.id == toId }
            if (from < 0 || to < 0 || from == to) return@update state

            val reordered = state.draft.toMutableList()
            reordered.add(to, reordered.removeAt(from))
            state.copy(draft = reordered)
        }
    }

    /**
     * Back to the twenty answers the toy ships with.
     *
     * The draft is seeded here as well as the source being set, because the settings flow only
     * reseeds on a change: a reset while the classic set is already the source would otherwise leave
     * every edit in place.
     */
    fun resetToClassic() {
        val classic = ballAnswerProvider.classic().map { answer -> answer.text }
        _uiState.update { state ->
            state.copy(draft = classic.map { text -> BallAnswerDraft(nextDraftId++, text) })
        }
        viewModelScope.launch {
            settingsRepository.setBallAnswerSourceId(BallAnswerSource.Classic.settingsId)
        }
    }

    /** A saved list is a source too: the ball follows it until another one is picked. */
    fun usePreset(preset: ListPreset) {
        viewModelScope.launch {
            settingsRepository.setBallAnswerSourceId(preset.id)
            listPresetRepository.markUsed(preset.id)
            if (preset.items.size > BALL_MAX_ANSWERS) {
                notify(R.string.ball_answers_trimmed, BALL_MAX_ANSWERS)
            }
        }
    }

    /**
     * Copies items into the editor rather than following them: a team's names and the quick
     * templates are not sources the ball can keep up with, so they land in the draft and are only
     * stored once saved.
     */
    fun applyItems(items: List<String>) {
        val kept = items
            .map(String::trim)
            .filter(String::isNotEmpty)
            .take(BALL_MAX_ANSWERS)
        val draft = kept.map { text -> BallAnswerDraft(nextDraftId++, text.take(BALL_MAX_ANSWER_LENGTH)) }

        _uiState.update { state -> state.copy(draft = draft) }

        if (items.count { it.isNotBlank() } > kept.size) {
            notify(R.string.ball_answers_trimmed, BALL_MAX_ANSWERS)
        }
    }

    fun save() {
        val items = _uiState.value.takeIf { it.canSave }?.cleaned ?: return
        viewModelScope.launch {
            settingsRepository.setBallCustomAnswers(items)
            settingsRepository.setBallAnswerSourceId(BallAnswerSource.Custom(items).settingsId)
            // Only once both writes are through, so leaving the screen cannot cut them short.
            _effects.send(BallAnswersUiEffect.Saved)
        }
    }

    private fun notify(messageRes: Int, formatArg: Int? = null) {
        viewModelScope.launch {
            _effects.send(BallAnswersUiEffect.ShowMessage(messageRes, formatArg))
        }
    }
}
