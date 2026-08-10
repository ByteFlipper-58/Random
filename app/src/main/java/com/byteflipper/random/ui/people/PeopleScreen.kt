package com.byteflipper.random.ui.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.person.PersonGender
import com.byteflipper.random.data.person.formatBirth
import com.byteflipper.random.data.person.formatBirthDate
import com.byteflipper.random.ui.components.EmptyState
import com.byteflipper.random.ui.components.EmptyStateButton
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.ui.theme.ShapesTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(onBack: () -> Unit) {
    val viewModel: PeopleViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PeopleUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = context.getString(effect.messageRes))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.people)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            SizedFab(
                size = settings.fabSize,
                onClick = viewModel::startCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.person_add_24px),
                    contentDescription = stringResource(R.string.add_person)
                )
            }
        }
    ) { innerPadding ->
        PeopleContent(
            innerPadding = innerPadding,
            people = uiState.people,
            onEdit = viewModel::startEdit,
            onAdd = viewModel::startCreate
        )
    }

    uiState.editState?.let { editState ->
        PersonEditorSheet(
            state = editState,
            onDismiss = viewModel::dismissEditor,
            onNameChange = viewModel::updateName,
            onGenderChange = viewModel::updateGender,
            onBirthYearChange = viewModel::updateBirthYear,
            onBirthDateChange = viewModel::updateBirthDate,
            onSave = viewModel::save,
            onDelete = viewModel::deleteCurrentPerson
        )
    }
}

@Composable
private fun PeopleContent(
    innerPadding: PaddingValues,
    people: List<Person>,
    onEdit: (Person) -> Unit,
    onAdd: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val duplicateNames = remember(people) {
        people
            .groupBy { it.displayName.trim().lowercase() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys
    }
    val filteredPeople = remember(people, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            people
        } else {
            people.filter { it.displayName.contains(normalizedQuery, ignoreCase = true) }
        }
    }

    if (people.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                iconRes = R.drawable.person_24px,
                title = stringResource(R.string.people_empty),
                description = stringResource(R.string.people_empty_hint),
                action = {
                    EmptyStateButton(
                        iconRes = R.drawable.person_add_24px,
                        label = stringResource(R.string.add_person),
                        onClick = onAdd
                    )
                }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 100.dp
        )
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text(text = stringResource(R.string.search_people)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search_24px),
                        contentDescription = null
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (filteredPeople.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.no_people_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
        itemsIndexed(filteredPeople, key = { _, person -> person.id }) { index, person ->
            Column(modifier = Modifier.animateItem()) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                PersonRow(
                    person = person,
                    isDuplicate = person.displayName.trim().lowercase() in duplicateNames,
                    onClick = { onEdit(person) }
                )
            }
        }
    }
}

@Composable
private fun PersonRow(
    person: Person,
    isDuplicate: Boolean,
    onClick: () -> Unit
) {
    val subtitle = personSubtitle(person, isDuplicate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = ShapesTokens.MediumShape,
            color = if (isDuplicate) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(personGenderIcon(person.gender)),
                    contentDescription = null,
                    tint = if (isDuplicate) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.arrow_forward_ios_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditorSheet(
    state: PersonEditState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onGenderChange: (PersonGender) -> Unit,
    onBirthYearChange: (String) -> Unit,
    onBirthDateChange: (Long?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(
                    if (state.personId == null) R.string.add_person else R.string.edit_person
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.person_name)) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            BirthDateField(
                birthYearText = state.birthYearText,
                birthDateEpochDay = state.birthDateEpochDay,
                onBirthYearChange = onBirthYearChange,
                onBirthDateChange = onBirthDateChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.person_gender),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            GenderSegmentedSelector(
                selectedGender = state.gender,
                onGenderChange = onGenderChange
            )
            Spacer(modifier = Modifier.height(22.dp))
            if (state.personId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person_remove_24px),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.delete),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person_edit_24px),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person_add_24px),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = stringResource(R.string.add),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(
    birthYearText: String,
    birthDateEpochDay: Long?,
    onBirthYearChange: (String) -> Unit,
    onBirthDateChange: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val hasFullDate = birthDateEpochDay != null

    OutlinedTextField(
        value = if (birthDateEpochDay != null) {
            formatBirthDate(birthDateEpochDay, LocalLocale.current.platformLocale)
        } else {
            birthYearText
        },
        onValueChange = onBirthYearChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = stringResource(
                    if (hasFullDate) R.string.person_birth_date else R.string.person_birth_year
                )
            )
        },
        singleLine = true,
        readOnly = hasFullDate,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    painter = painterResource(R.drawable.calendar_month_24px),
                    contentDescription = stringResource(R.string.pick_birth_date)
                )
            }
        }
    )

    if (showDatePicker) {
        val initialMillis = birthDateEpochDay?.let { epochDay ->
            LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onBirthDateChange(
                            datePickerState.selectedDateMillis?.let { millis ->
                                // DatePicker reports UTC midnight; read the date back in UTC.
                                Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
                            }
                        )
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun personGenderLabel(gender: PersonGender): String = when (gender) {
    PersonGender.Unspecified -> stringResource(R.string.person_gender_unspecified)
    PersonGender.Male -> stringResource(R.string.person_gender_male)
    PersonGender.Female -> stringResource(R.string.person_gender_female)
}

fun personGenderIcon(gender: PersonGender): Int = when (gender) {
    PersonGender.Unspecified -> R.drawable.question_mark_24px
    PersonGender.Male -> R.drawable.male_24px
    PersonGender.Female -> R.drawable.female_24px
}

@Composable
private fun GenderSegmentedSelector(
    selectedGender: PersonGender,
    onGenderChange: (PersonGender) -> Unit
) {
    // Only Male/Female are offered; re-tapping the active one clears back to Unspecified.
    val options = listOf(PersonGender.Male, PersonGender.Female)

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, gender ->
            val selected = selectedGender == gender
            SegmentedButton(
                selected = selected,
                onClick = {
                    onGenderChange(if (selected) PersonGender.Unspecified else gender)
                },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                ),
                icon = {
                    Icon(
                        painter = painterResource(personGenderIcon(gender)),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                Text(
                    text = personGenderLabel(gender),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun personSubtitle(person: Person, isDuplicate: Boolean): String {
    val parts = buildList {
        add(personGenderLabel(person.gender))
        person.formatBirth(LocalLocale.current.platformLocale)?.let { add(it) }
        if (isDuplicate) add(stringResource(R.string.duplicate_name))
    }.filter { it.isNotBlank() }
    return parts.joinToString(" • ")
}
