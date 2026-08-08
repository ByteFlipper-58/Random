package com.byteflipper.random.ui.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.EmptyState
import com.byteflipper.random.ui.components.EmptyStateButton
import com.byteflipper.random.ui.teams.components.PersonListRow

@Composable
fun TeamsContent(
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    uiState: TeamsUiState,
    onPickMembers: () -> Unit,
    onRemovePerson: (Long) -> Unit
) {
    val peopleById = remember(uiState.people) { uiState.people.associateBy { it.id } }
    val selectedPeople = remember(uiState.editor.selectedMemberIds, peopleById) {
        uiState.editor.selectedMemberIds.mapNotNull { peopleById[it] }
    }

    if (selectedPeople.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.people.isNotEmpty()) {
                EmptyState(
                    iconRes = R.drawable.group_add_24px,
                    title = stringResource(R.string.teams_empty_title),
                    description = stringResource(R.string.teams_empty_hint),
                    action = {
                        EmptyStateButton(
                            iconRes = R.drawable.group_add_24px,
                            label = stringResource(R.string.pick_people),
                            onClick = onPickMembers
                        )
                    }
                )
            } else {
                EmptyState(
                    iconRes = R.drawable.person_add_24px,
                    title = stringResource(R.string.teams_no_people_title),
                    description = stringResource(R.string.teams_no_people_hint),
                    action = {
                        EmptyStateButton(
                            iconRes = R.drawable.person_add_24px,
                            label = stringResource(R.string.add_people),
                            onClick = onPickMembers
                        )
                    }
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 112.dp
        )
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.selected_members),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.selected_members_count, selectedPeople.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onPickMembers) {
                    Icon(
                        painter = painterResource(R.drawable.group_add_24px),
                        contentDescription = stringResource(R.string.pick_people)
                    )
                }
            }
        }

        itemsIndexed(selectedPeople, key = { _, person -> person.id }) { index, person ->
            Column(modifier = Modifier.animateItem()) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                PersonListRow(
                    person = person,
                    actionIconRes = R.drawable.person_remove_24px,
                    actionContentDescription = stringResource(R.string.remove_person),
                    onAction = { onRemovePerson(person.id) }
                )
            }
        }
    }
}
