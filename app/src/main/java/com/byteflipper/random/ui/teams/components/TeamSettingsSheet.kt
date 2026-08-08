package com.byteflipper.random.ui.teams.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.team.TeamSplitMode
import com.byteflipper.random.ui.components.ConfigDivider
import com.byteflipper.random.ui.components.ConfigHeader
import com.byteflipper.random.ui.components.ConfigRadioOption
import com.byteflipper.random.ui.components.ConfigSection
import com.byteflipper.random.ui.components.DelayConfig
import com.byteflipper.random.ui.components.DelaySection
import com.byteflipper.random.ui.teams.TeamPresetEditorState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TeamSettingsSheet(
    visible: Boolean,
    editorState: TeamPresetEditorState,
    onDismiss: () -> Unit,
    onSplitModeChange: (TeamSplitMode) -> Unit,
    onTeamCountChange: (String) -> Unit,
    onGroupSizeChange: (String) -> Unit,
    onEqualTeamSizesOnlyChange: (Boolean) -> Unit,
    onBalanceByGenderChange: (Boolean) -> Unit,
    onBalanceByAgeChange: (Boolean) -> Unit,
    onUseDelayChange: (Boolean) -> Unit,
    onDelayChange: (String) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val selectedCount = editorState.selectedMemberIds.size
    val maxTeamCount = minOf(8, selectedCount.coerceAtLeast(2))
    val teamCountValue = editorState.teamCountText.toIntOrNull()?.coerceIn(2, maxTeamCount) ?: 2
    val maxGroupSize = selectedCount.coerceAtLeast(2)
    val groupSizeValue = editorState.groupSizeText.toIntOrNull()?.coerceIn(2, maxGroupSize) ?: 2

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    modifier = Modifier.size(width = 32.dp, height = 4.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ConfigHeader()
            ConfigDivider()

            ConfigSection(
                icon = painterResource(id = R.drawable.group_24px),
                title = stringResource(R.string.split_config_section),
                description = stringResource(R.string.team_settings_hint, selectedCount)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ConfigRadioOption(
                        selected = editorState.splitMode == TeamSplitMode.TeamCount,
                        title = stringResource(R.string.split_by_team_count),
                        onClick = { onSplitModeChange(TeamSplitMode.TeamCount) }
                    )
                    ConfigRadioOption(
                        selected = editorState.splitMode == TeamSplitMode.GroupSize,
                        title = stringResource(R.string.split_by_group_size),
                        onClick = { onSplitModeChange(TeamSplitMode.GroupSize) }
                    )
                }
            }

            ConfigDivider()

            when (editorState.splitMode) {
                TeamSplitMode.TeamCount -> {
                    ConfigSection(
                        icon = painterResource(id = R.drawable.groups_24px),
                        title = stringResource(R.string.team_count),
                        description = stringResource(R.string.team_count_supporting)
                    ) {
                        TeamSettingsSliderSection(
                            value = teamCountValue,
                            minValue = 2,
                            maxValue = maxTeamCount,
                            onValueChange = { onTeamCountChange(it.toString()) }
                        )
                    }

                    ConfigDivider()

                    ConfigSection(
                        icon = painterResource(id = R.drawable.group_remove_24px),
                        title = stringResource(R.string.equal_teams_only),
                        description = stringResource(R.string.equal_teams_only_supporting),
                        action = {
                            Switch(
                                checked = editorState.equalTeamSizesOnly,
                                onCheckedChange = onEqualTeamSizesOnlyChange
                            )
                        }
                    )
                }

                TeamSplitMode.GroupSize -> {
                    ConfigSection(
                        icon = painterResource(id = R.drawable.person_24px),
                        title = stringResource(R.string.group_size),
                        description = stringResource(R.string.group_size_supporting)
                    ) {
                        TeamSettingsSliderSection(
                            value = groupSizeValue,
                            minValue = 2,
                            maxValue = maxGroupSize,
                            onValueChange = { onGroupSizeChange(it.toString()) }
                        )
                    }
                }
            }

            ConfigDivider()

            ConfigSection(
                icon = painterResource(id = R.drawable.male_24px),
                title = stringResource(R.string.balance_by_gender),
                description = stringResource(R.string.balance_by_gender_supporting),
                action = {
                    Switch(
                        checked = editorState.balanceByGender,
                        onCheckedChange = onBalanceByGenderChange
                    )
                }
            )

            ConfigDivider()

            ConfigSection(
                icon = painterResource(id = R.drawable.calendar_month_24px),
                title = stringResource(R.string.balance_by_age),
                description = stringResource(R.string.balance_by_age_supporting),
                action = {
                    Switch(
                        checked = editorState.balanceByAge,
                        onCheckedChange = onBalanceByAgeChange
                    )
                }
            )

            ConfigDivider()

            DelaySection(
                delayConfig = DelayConfig(
                    useDelay = editorState.useDelay,
                    onUseDelayChange = onUseDelayChange,
                    delayText = editorState.delayText,
                    onDelayChange = onDelayChange
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TeamSettingsSliderSection(
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { onValueChange(minValue) },
                label = { Text(minValue.toString()) }
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            AssistChip(
                onClick = { onValueChange(maxValue) },
                label = { Text(maxValue.toString()) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (minValue < maxValue) {
            Slider(
                value = value.toFloat(),
                onValueChange = { nextValue ->
                    onValueChange(nextValue.roundToInt().coerceIn(minValue, maxValue))
                },
                valueRange = minValue.toFloat()..maxValue.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
