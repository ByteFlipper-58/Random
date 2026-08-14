package com.byteflipper.random.ui.teams.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.data.team.TeamPresetWithCount
import com.byteflipper.random.data.team.TeamSplitMode
import com.byteflipper.random.ui.theme.ShapesTokens

@Composable
fun TeamPresetRow(
    item: TeamPresetWithCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = item.preset
    val isEmpty = item.aliveMemberCount == 0

    val splitInfo = if (preset.splitMode == TeamSplitMode.TeamCount) {
        stringResource(R.string.team_preset_team_count_value, preset.teamCount ?: 0)
    } else {
        stringResource(R.string.team_preset_group_size_value, preset.groupSize ?: 0)
    }
    val memberInfo = if (isEmpty) {
        stringResource(R.string.team_preset_no_members)
    } else {
        stringResource(R.string.team_members_count_value, item.aliveMemberCount)
    }

    SummaryRowCard(
        modifier = modifier,
        iconRes = R.drawable.group_24px,
        title = preset.name,
        subtitle = "$splitInfo · $memberInfo",
        accentContainer = if (isEmpty) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        accentContent = if (isEmpty) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        subtitleColor = if (isEmpty) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        onClick = onClick
    )
}

@Composable
fun SummaryRowCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    accentContainer: androidx.compose.ui.graphics.Color,
    accentContent: androidx.compose.ui.graphics.Color,
    subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(ShapesTokens.CardShape),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        shape = ShapesTokens.CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(ShapesTokens.CardShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = ShapesTokens.MediumShape,
                color = accentContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accentContent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
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
}
