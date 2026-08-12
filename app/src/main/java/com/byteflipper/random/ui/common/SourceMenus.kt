package com.byteflipper.random.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import kotlinx.coroutines.launch

/** Sections of a source menu. A single menu switches between them instead of separate buttons. */
enum class SourceMenuSection {
    Root,
    People,
    Templates,
    Presets
}

/** Takes the menu back to its root section. */
@Composable
fun MenuBackItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.back)) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.arrow_back_24px),
                contentDescription = null
            )
        }
    )
    HorizontalDivider()
}

/** Entry that opens the people section. */
@Composable
fun PeopleSectionItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.people)) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.groups_24px),
                contentDescription = null
            )
        }
    )
}

/**
 * Entries of the people section: either everybody, or one of the participant sets saved in Teams.
 *
 * [minItems] disables entries that would not be enough for the mode, since the wheel needs at
 * least two items.
 */
@Composable
fun PeopleSourceMenuItems(
    minItems: Int = 1,
    onPeopleSelected: (sourceName: String, names: List<String>) -> Unit
) {
    val viewModel: PeopleSourceViewModel = hiltViewModel()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val teamPresets by viewModel.teamPresets.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val allPeopleLabel = stringResource(R.string.people_source_all)

    if (people.isEmpty()) {
        DropdownMenuItem(
            enabled = false,
            text = { Text(stringResource(R.string.people_empty)) },
            onClick = {}
        )
        return
    }

    DropdownMenuItem(
        enabled = people.size >= minItems,
        text = {
            Column {
                Text(allPeopleLabel, fontWeight = FontWeight.Medium)
                Text(
                    text = stringResource(R.string.team_members_count_value, people.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.groups_24px),
                contentDescription = null
            )
        },
        onClick = { onPeopleSelected(allPeopleLabel, people.map { it.displayName }) }
    )

    teamPresets.forEach { entry ->
        DropdownMenuItem(
            enabled = entry.aliveMemberCount >= minItems,
            text = {
                Column {
                    Text(entry.preset.name, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (entry.aliveMemberCount == 0) {
                            stringResource(R.string.team_preset_no_members)
                        } else {
                            stringResource(
                                R.string.team_members_count_value,
                                entry.aliveMemberCount
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.list_alt_24px),
                    contentDescription = null
                )
            },
            onClick = {
                // Membership lives in its own table, so it is read on demand.
                scope.launch {
                    val names = viewModel.memberNames(entry.preset.id)
                    if (names.isNotEmpty()) {
                        viewModel.markTeamPresetUsed(entry.preset.id)
                        onPeopleSelected(entry.preset.name, names)
                    }
                }
            }
        )
    }
}

/** Entries of the templates section. */
@Composable
fun QuickTemplateMenuItems(onTemplateSelected: (QuickTemplate) -> Unit) {
    rememberQuickTemplates().forEach { template ->
        DropdownMenuItem(
            text = {
                Column {
                    Text(template.name, fontWeight = FontWeight.Medium)
                    Text(
                        text = template.items.take(4).joinToString(", ") +
                            if (template.items.size > 4) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = { onTemplateSelected(template) }
        )
    }
}
