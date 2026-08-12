package com.byteflipper.random.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.person.PeopleRepository
import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.team.TeamPresetRepository
import com.byteflipper.random.data.team.TeamPresetWithCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * People and saved participant sets as a source of items for other modes.
 *
 * A separate small ViewModel rather than an addition to the existing ones: both the wheel and the
 * list need the same people, and [com.byteflipper.random.ui.teams.TeamsViewModel] does not fit
 * because it is created with a preset id.
 */
@HiltViewModel
class PeopleSourceViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val teamPresetRepository: TeamPresetRepository
) : ViewModel() {

    val people: StateFlow<List<Person>> = peopleRepository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val teamPresets: StateFlow<List<TeamPresetWithCount>> =
        teamPresetRepository.observeAllWithCounts().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Member names of a set. Membership lives in its own table, so the query is on demand. */
    suspend fun memberNames(presetId: Long): List<String> =
        teamPresetRepository.getById(presetId)?.members?.map { it.displayName } ?: emptyList()

    fun markTeamPresetUsed(presetId: Long) {
        viewModelScope.launch { teamPresetRepository.markUsed(presetId) }
    }
}
