package com.byteflipper.random.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.R
import com.byteflipper.random.data.person.PeopleRepository
import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.team.TeamPreset
import com.byteflipper.random.data.team.TeamPresetRepository
import com.byteflipper.random.data.team.TeamSplitMode
import com.byteflipper.random.data.team.TeamPresetWithCount
import com.byteflipper.random.domain.team.TeamGenerationResult
import com.byteflipper.random.domain.team.usecase.GenerateTeamsUseCase
import com.byteflipper.random.domain.team.usecase.ValidateTeamPresetInputsUseCase
import com.byteflipper.random.utils.Constants.DEFAULT_DELAY_MS
import com.byteflipper.random.utils.Constants.INSTANT_DELAY_MS
import com.byteflipper.random.utils.Constants.MAX_DELAY_MS
import com.byteflipper.random.utils.Constants.MIN_DELAY_MS
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamPresetEditorState(
    val presetId: Long? = null,
    val name: String = "",
    val splitMode: TeamSplitMode = TeamSplitMode.TeamCount,
    val teamCountText: String = "2",
    val groupSizeText: String = "2",
    val equalTeamSizesOnly: Boolean = false,
    val balanceByGender: Boolean = false,
    val balanceByAge: Boolean = false,
    val delayText: String = DEFAULT_DELAY_MS.toString(),
    val useDelay: Boolean = true,
    val selectedMemberIds: List<Long> = emptyList()
)

data class TeamGenerationState(
    val result: TeamGenerationResult? = null,
    val cardColorSeed: Long? = null
)

data class TeamsUiState(
    val people: List<Person> = emptyList(),
    val presets: List<TeamPresetWithCount> = emptyList(),
    val editor: TeamPresetEditorState = TeamPresetEditorState(),
    val generation: TeamGenerationState = TeamGenerationState()
)

sealed interface TeamsUiEffect {
    data class ShowSnackbar(val messageRes: Int) : TeamsUiEffect
}

@HiltViewModel(assistedFactory = TeamsViewModel.Factory::class)
class TeamsViewModel @AssistedInject constructor(
    @Assisted private val presetId: Long?,
    private val peopleRepository: PeopleRepository,
    private val teamPresetRepository: TeamPresetRepository,
    private val settingsRepository: SettingsRepository,
    private val validateTeamPresetInputs: ValidateTeamPresetInputsUseCase,
    private val generateTeams: GenerateTeamsUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(presetId: Long?): TeamsViewModel
    }

    private val _uiState = MutableStateFlow(TeamsUiState())
    val uiState: StateFlow<TeamsUiState> = _uiState.asStateFlow()

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    private val _effects = MutableSharedFlow<TeamsUiEffect>()
    val effects = _effects

    init {
        viewModelScope.launch {
            peopleRepository.observeAll().collect { people ->
                _uiState.update { state ->
                    val availableIds = people.mapTo(linkedSetOf()) { it.id }
                    val nextEditor = state.editor.copy(
                        selectedMemberIds = state.editor.selectedMemberIds.filter { it in availableIds }
                    ).normalized()
                    state.copy(
                        people = people,
                        editor = nextEditor
                    )
                }
            }
        }
        viewModelScope.launch {
            teamPresetRepository.observeAllWithCounts().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }
        presetId?.let(::openPreset)
    }

    fun updatePresetName(value: String) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(name = value),
                generation = TeamGenerationState()
            )
        }
    }

    fun setSplitMode(mode: TeamSplitMode) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(splitMode = mode),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateTeamCountText(value: String) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(teamCountText = value.filter { char -> char.isDigit() }).normalized(),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateGroupSizeText(value: String) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(groupSizeText = value.filter { char -> char.isDigit() }).normalized(),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateEqualTeamSizesOnly(value: Boolean) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(equalTeamSizesOnly = value),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateBalanceByGender(value: Boolean) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(balanceByGender = value),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateBalanceByAge(value: Boolean) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(balanceByAge = value),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateDelayText(value: String) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(delayText = value.filter { char -> char.isDigit() }),
                generation = TeamGenerationState()
            )
        }
    }

    fun updateUseDelay(useDelay: Boolean) {
        _uiState.update {
            it.copy(
                editor = it.editor.copy(useDelay = useDelay),
                generation = TeamGenerationState()
            )
        }
    }

    fun setPersonSelection(personIds: List<Long>) {
        _uiState.update { state ->
            state.copy(
                editor = state.editor.copy(selectedMemberIds = personIds.distinct()).normalized(),
                generation = TeamGenerationState()
            )
        }
    }

    fun togglePersonSelection(personId: Long) {
        _uiState.update { state ->
            val selected = state.editor.selectedMemberIds.toMutableList()
            if (personId in selected) {
                selected.remove(personId)
            } else {
                selected.add(personId)
            }
            state.copy(
                editor = state.editor.copy(selectedMemberIds = selected).normalized(),
                generation = TeamGenerationState()
            )
        }
    }

    fun createNewPresetDraft() {
        _uiState.update {
            it.copy(
                editor = TeamPresetEditorState(),
                generation = TeamGenerationState()
            )
        }
    }

    fun openPreset(presetId: Long) {
        viewModelScope.launch {
            val presetWithMembers = teamPresetRepository.getById(presetId) ?: return@launch
            _uiState.update {
                it.copy(
                    editor = TeamPresetEditorState(
                        presetId = presetWithMembers.preset.id,
                        name = presetWithMembers.preset.name,
                        splitMode = presetWithMembers.preset.splitMode,
                        teamCountText = presetWithMembers.preset.teamCount?.toString() ?: "",
                        groupSizeText = presetWithMembers.preset.groupSize?.toString() ?: "",
                        equalTeamSizesOnly = presetWithMembers.preset.equalTeamSizesOnly,
                        balanceByGender = presetWithMembers.preset.balanceByGender,
                        balanceByAge = presetWithMembers.preset.balanceByAge,
                        selectedMemberIds = presetWithMembers.members
                            .sortedBy { member -> member.position }
                            .map { member -> member.personId }
                    ).normalized(),
                    generation = TeamGenerationState()
                )
            }
        }
    }

    fun savePreset(nameOverride: String? = null) {
        val state = _uiState.value
        val name = (nameOverride ?: state.editor.name).trim()
        if (name.isEmpty()) {
            emitEffect(TeamsUiEffect.ShowSnackbar(R.string.team_preset_requires_name))
            return
        }

        val validation = validateCurrentConfig() ?: return
        val existing = state.editor.presetId?.let { presetId ->
            state.presets.firstOrNull { it.preset.id == presetId }?.preset
        }
        val now = System.currentTimeMillis()
        val preset = TeamPreset(
            id = existing?.id ?: 0,
            name = name,
            splitMode = validation.splitMode,
            teamCount = validation.teamCount,
            groupSize = validation.groupSize,
            equalTeamSizesOnly = state.editor.equalTeamSizesOnly,
            balanceByGender = state.editor.balanceByGender,
            balanceByAge = state.editor.balanceByAge,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            lastUsedAt = existing?.lastUsedAt,
            useCount = existing?.useCount ?: 0,
            isPinned = existing?.isPinned ?: false
        )

        viewModelScope.launch {
            val presetId = teamPresetRepository.upsertPreset(preset, state.editor.selectedMemberIds)
            _uiState.update {
                it.copy(editor = it.editor.copy(presetId = presetId, name = name))
            }
            emitEffect(TeamsUiEffect.ShowSnackbar(R.string.team_preset_saved))
        }
    }

    fun prepareGeneration(): Boolean {
        val validation = validateCurrentConfig() ?: return false
        val state = _uiState.value
        val peopleById = state.people.associateBy { it.id }
        val participants = state.editor.selectedMemberIds.mapNotNull { personId -> peopleById[personId] }
        val result = generateTeams(
            GenerateTeamsUseCase.Params(
                participants = participants,
                splitMode = validation.splitMode,
                teamCount = validation.teamCount,
                groupSize = validation.groupSize,
                equalTeamSizesOnly = state.editor.equalTeamSizesOnly,
                balanceByGender = state.editor.balanceByGender,
                balanceByAge = state.editor.balanceByAge
            )
        )
        _uiState.update {
            it.copy(
                generation = TeamGenerationState(
                    result = result,
                    cardColorSeed = kotlin.random.Random.nextLong()
                )
            )
        }
        state.editor.presetId?.let { presetId ->
            viewModelScope.launch {
                teamPresetRepository.markUsed(presetId)
            }
        }
        return true
    }

    fun clearGeneration() {
        _uiState.update { it.copy(generation = TeamGenerationState()) }
    }

    fun getEffectiveDelayMs(): Int {
        val editor = _uiState.value.editor
        return if (editor.useDelay) {
            editor.delayText.toIntOrNull()?.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS) ?: DEFAULT_DELAY_MS
        } else {
            INSTANT_DELAY_MS
        }
    }

    private fun validateCurrentConfig(): ValidateTeamPresetInputsUseCase.Result.Success? {
        val state = _uiState.value
        return when (
            val result = validateTeamPresetInputs(
                ValidateTeamPresetInputsUseCase.Params(
                    participantCount = state.editor.selectedMemberIds.size,
                    splitMode = state.editor.splitMode,
                    teamCountText = state.editor.teamCountText,
                    groupSizeText = state.editor.groupSizeText
                )
            )
        ) {
            is ValidateTeamPresetInputsUseCase.Result.Error -> {
                emitEffect(TeamsUiEffect.ShowSnackbar(result.messageRes))
                null
            }

            is ValidateTeamPresetInputsUseCase.Result.Success -> result
        }
    }

    private fun emitEffect(effect: TeamsUiEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    private fun TeamPresetEditorState.normalized(): TeamPresetEditorState {
        val selectedCount = selectedMemberIds.size
        val maxTeams = minOf(ValidateTeamPresetInputsUseCase.MAX_TEAM_COUNT, selectedCount.coerceAtLeast(2))
        val normalizedTeamCount = teamCountText
            .toIntOrNull()
            ?.coerceIn(2, maxTeams)
            ?.toString()
            ?: teamCountText
        val maxGroupSize = selectedCount.coerceAtLeast(2)
        val normalizedGroupSize = groupSizeText
            .toIntOrNull()
            ?.coerceIn(2, maxGroupSize)
            ?.toString()
            ?: groupSizeText
        return copy(
            teamCountText = normalizedTeamCount,
            groupSizeText = normalizedGroupSize
        )
    }
}
