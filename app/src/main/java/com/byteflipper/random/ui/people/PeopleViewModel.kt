package com.byteflipper.random.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.R
import com.byteflipper.random.data.person.PeopleRepository
import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.person.PersonGender
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

data class PersonEditState(
    val personId: Long? = null,
    val name: String = "",
    val gender: PersonGender = PersonGender.Unspecified,
    val birthYearText: String = "",
    val birthDateEpochDay: Long? = null
)

data class PeopleUiState(
    val people: List<Person> = emptyList(),
    val editState: PersonEditState? = null
)

sealed interface PeopleUiEffect {
    data class ShowSnackbar(val messageRes: Int) : PeopleUiEffect
}

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PeopleUiState())
    val uiState: StateFlow<PeopleUiState> = _uiState.asStateFlow()

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.byteflipper.random.data.settings.Settings()
    )

    private val _effects = MutableSharedFlow<PeopleUiEffect>()
    val effects = _effects

    init {
        viewModelScope.launch {
            peopleRepository.observeAll().collect { people ->
                _uiState.update { it.copy(people = people) }
            }
        }
    }

    fun startCreate() {
        _uiState.update { it.copy(editState = PersonEditState()) }
    }

    fun startEdit(person: Person) {
        _uiState.update {
            it.copy(
                editState = PersonEditState(
                    personId = person.id,
                    name = person.displayName,
                    gender = person.gender,
                    birthYearText = person.birthYear?.toString().orEmpty(),
                    birthDateEpochDay = person.birthDateEpochDay
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editState = null) }
    }

    fun updateName(value: String) {
        _uiState.update { state ->
            state.copy(editState = state.editState?.copy(name = value))
        }
    }

    fun updateGender(value: PersonGender) {
        _uiState.update { state ->
            state.copy(editState = state.editState?.copy(gender = value))
        }
    }

    /** Typing a year by hand clears any previously picked full date. */
    fun updateBirthYear(value: String) {
        _uiState.update { state ->
            state.copy(
                editState = state.editState?.copy(
                    birthYearText = value.filter { it.isDigit() }.take(4),
                    birthDateEpochDay = null
                )
            )
        }
    }

    fun updateBirthDate(epochDay: Long?) {
        _uiState.update { state ->
            val year = epochDay?.let { LocalDate.ofEpochDay(it).year }
            state.copy(
                editState = state.editState?.copy(
                    birthDateEpochDay = epochDay,
                    birthYearText = year?.toString().orEmpty()
                )
            )
        }
    }

    fun save() {
        val editState = _uiState.value.editState ?: return
        val name = editState.name.trim()
        if (name.isEmpty()) {
            emitEffect(PeopleUiEffect.ShowSnackbar(R.string.person_requires_name))
            return
        }

        val birthYear: Int?
        val birthDateEpochDay: Long?

        if (editState.birthDateEpochDay != null) {
            birthDateEpochDay = editState.birthDateEpochDay
            birthYear = LocalDate.ofEpochDay(editState.birthDateEpochDay).year
        } else {
            birthDateEpochDay = null
            birthYear = if (editState.birthYearText.isBlank()) {
                null
            } else {
                editState.birthYearText.toIntOrNull()
            }
        }

        val currentYear = Year.now().value
        if (birthYear != null && birthYear !in 1900..currentYear) {
            emitEffect(PeopleUiEffect.ShowSnackbar(R.string.person_invalid_birth_year))
            return
        }

        val existing = editState.personId?.let { id ->
            _uiState.value.people.firstOrNull { it.id == id }
        }
        val now = System.currentTimeMillis()
        val person = Person(
            id = existing?.id ?: 0,
            displayName = name,
            gender = editState.gender,
            birthYear = birthYear,
            birthDateEpochDay = birthDateEpochDay,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            isArchived = existing?.isArchived ?: false
        )
        viewModelScope.launch {
            peopleRepository.upsert(person)
            _uiState.update {
                it.copy(
                    editState = if (editState.personId == null) {
                        PersonEditState()
                    } else {
                        null
                    }
                )
            }
            emitEffect(PeopleUiEffect.ShowSnackbar(R.string.person_saved))
        }
    }

    fun deleteCurrentPerson() {
        val editState = _uiState.value.editState ?: return
        val personId = editState.personId ?: return
        viewModelScope.launch {
            peopleRepository.archive(personId)
            _uiState.update { it.copy(editState = null) }
            emitEffect(PeopleUiEffect.ShowSnackbar(R.string.person_deleted))
        }
    }

    private fun emitEffect(effect: PeopleUiEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }
}
