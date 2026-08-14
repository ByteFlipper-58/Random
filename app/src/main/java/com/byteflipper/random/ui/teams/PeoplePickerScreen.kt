package com.byteflipper.random.ui.teams

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.EmptyState
import com.byteflipper.random.ui.components.EmptyStateButton
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.ui.people.PeopleUiEffect
import com.byteflipper.random.ui.people.PeopleViewModel
import com.byteflipper.random.ui.people.PersonEditorSheet
import com.byteflipper.random.ui.teams.components.PersonListRow

/**
 * Full-screen participant picker. Tapping a row adds the person and returns to Teams;
 * the trailing "+" adds without leaving, so several people can be picked in a row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeoplePickerScreen(
    selectedMemberIds: List<Long>,
    onSelectionChanged: (List<Long>) -> Unit,
    onBack: () -> Unit
) {
    val peopleViewModel: PeopleViewModel = hiltViewModel()
    val peopleState by peopleViewModel.uiState.collectAsStateWithLifecycle()
    val settings by peopleViewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        peopleViewModel.effects.collect { effect ->
            when (effect) {
                is PeopleUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = context.getString(effect.messageRes))
                }
            }
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim()
    var selectedIds by rememberSaveable(selectedMemberIds) { mutableStateOf(selectedMemberIds) }
    val unselected = remember(peopleState.people, selectedIds) {
        peopleState.people.filter { it.id !in selectedIds }
    }
    val available = remember(unselected, normalizedQuery) {
        unselected.filter {
            normalizedQuery.isBlank() || it.displayName.contains(normalizedQuery, ignoreCase = true)
        }
    }

    var isBackHandled by remember { mutableStateOf(false) }
    val safeBack = {
        if (!isBackHandled) {
            isBackHandled = true
            onBack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.pick_members_title)) },
                navigationIcon = {
                    IconButton(onClick = safeBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            SizedFab(
                size = settings.fabSize,
                onClick = peopleViewModel::startCreate,
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
        // Nothing left to pick: searching makes no sense, so the whole screen becomes the empty state.
        if (unselected.isEmpty()) {
            val hasPeople = peopleState.people.isNotEmpty()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    iconRes = if (hasPeople) R.drawable.groups_24px else R.drawable.person_24px,
                    title = stringResource(
                        if (hasPeople) R.string.people_picker_all_selected_title else R.string.people_empty
                    ),
                    description = stringResource(
                        if (hasPeople) R.string.people_picker_all_selected else R.string.people_picker_empty_hint
                    ),
                    action = {
                        EmptyStateButton(
                            iconRes = R.drawable.person_add_24px,
                            label = stringResource(R.string.add_person),
                            onClick = peopleViewModel::startCreate
                        )
                    }
                )
            }
            return@Scaffold
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

            if (available.isEmpty()) {
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

            itemsIndexed(available, key = { _, person -> person.id }) { index, person ->
                Column(modifier = Modifier.animateItem()) {
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    PersonListRow(
                        person = person,
                        actionIconRes = R.drawable.add_24px,
                        actionContentDescription = stringResource(R.string.add_member),
                        onAction = {
                            val updated = (selectedIds + person.id).distinct()
                            selectedIds = updated
                            onSelectionChanged(updated)
                        },
                        onClick = {
                            val updated = (selectedIds + person.id).distinct()
                            selectedIds = updated
                            onSelectionChanged(updated)
                        }
                    )
                }
            }
        }
    }

    peopleState.editState?.let { state ->
        PersonEditorSheet(
            state = state,
            onDismiss = peopleViewModel::dismissEditor,
            onNameChange = peopleViewModel::updateName,
            onGenderChange = peopleViewModel::updateGender,
            onBirthYearChange = peopleViewModel::updateBirthYear,
            onBirthDateChange = peopleViewModel::updateBirthDate,
            onSave = peopleViewModel::save,
            onDelete = peopleViewModel::deleteCurrentPerson
        )
    }
}
