package com.byteflipper.random.ui.teams.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.data.team.TeamSplitMode
import com.byteflipper.random.domain.team.GeneratedTeam
import com.byteflipper.random.domain.team.TeamGenerationResult
import com.byteflipper.random.ui.components.flip.FlipCardOverlay
import com.byteflipper.random.ui.components.flip.FlipCardState
import com.byteflipper.random.ui.theme.CardContentTheme
import com.byteflipper.random.ui.theme.getRainbowColors
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun TeamsFlipOverlay(
    result: TeamGenerationResult?,
    cardColorSeed: Long?,
    flipState: FlipCardState,
    anchorInRoot: Offset,
    snackbarHostState: SnackbarHostState,
    onClosed: () -> Unit,
    isGenerating: Boolean
) {
    if (result == null && !flipState.isVisible && flipState.scrimProgress.value <= 0.01f) return

    val context = LocalContext.current
    val rainbowColors = getRainbowColors()
    val animatedColor = remember { Animatable(Color.Transparent) }
    val targetColor = remember(cardColorSeed, result) {
        val random = cardColorSeed?.let(::Random) ?: Random
        rainbowColors[random.nextInt(rainbowColors.size)]
    }

    LaunchedEffect(targetColor) {
        if (animatedColor.value == Color.Transparent) {
            animatedColor.snapTo(targetColor)
        } else {
            animatedColor.animateTo(targetColor, tween(400))
        }
    }

    val configuration = LocalConfiguration.current
    val maxCardWidth = (configuration.screenWidthDp.dp - 32.dp).coerceAtLeast(220.dp)
    val maxCardHeight = (configuration.screenHeightDp.dp - 64.dp).coerceAtLeast(320.dp)
    val effectiveTeamsCount = if (isGenerating) 1 else (result?.teams?.size ?: 1)
    val targetCardWidth = when {
        effectiveTeamsCount <= 2 -> 310.dp
        effectiveTeamsCount <= 4 -> 324.dp
        else -> 336.dp
    }.coerceAtMost(maxCardWidth)
    val targetCardHeight = when {
        effectiveTeamsCount <= 2 -> 430.dp
        effectiveTeamsCount <= 4 -> 520.dp
        else -> 610.dp
    }.coerceIn(320.dp.coerceAtMost(maxCardHeight), maxCardHeight)

    val animatedCardWidth by animateDpAsState(
        targetValue = targetCardWidth,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "teams_overlay_width"
    )
    val animatedCardHeight by animateDpAsState(
        targetValue = targetCardHeight,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "teams_overlay_height"
    )

    FlipCardOverlay(
        state = flipState,
        anchorInRoot = anchorInRoot,
        onClosed = onClosed,
        cardSize = animatedCardWidth,
        cardHeight = animatedCardHeight,
        frontContainerColor = animatedColor.value,
        backContainerColor = animatedColor.value,
        onLongPress = {
            val generatedResult = result ?: return@FlipCardOverlay
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    context.getString(R.string.team_results_title),
                    formatTeamResultForClipboard(context, generatedResult)
                )
            )
        },
        frontContent = {
            CardContentTheme {
                TeamsResultsDisplay(
                    result = result,
                    cardColor = animatedColor.value,
                    isGenerating = isGenerating
                )
            }
        },
        backContent = {
            CardContentTheme {
                TeamsResultsDisplay(
                    result = result,
                    cardColor = animatedColor.value,
                    isGenerating = isGenerating
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(flipState.isVisible, result) {
        if (flipState.isVisible && result != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}

@Composable
private fun TeamsResultsDisplay(
    result: TeamGenerationResult?,
    cardColor: Color,
    isGenerating: Boolean
) {
    if (result == null || isGenerating) return

    val context = LocalContext.current
    val textColor = contrastColorFor(cardColor)
    var visibleTeams by remember(result) { mutableIntStateOf(0) }

    LaunchedEffect(result) {
        visibleTeams = 0
        result.teams.forEachIndexed { index, _ ->
            delay(110)
            visibleTeams = index + 1
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.team_results_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        item {
            Text(
                text = resultSubtitle(result),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.82f)
            )
        }
        items(result.teams, key = { it.index }) { team ->
            AnimatedVisibility(
                visible = visibleTeams >= team.index + 1,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn() + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialScale = 0.94f
                ),
                exit = shrinkVertically() + fadeOut()
            ) {
                TeamResultBlock(
                    team = team,
                    splitMode = result.splitMode,
                    textColor = textColor
                )
            }
        }
        if (result.leftOutMembers.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = visibleTeams >= result.teams.size,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                ) {
                    LeftOutResultBlock(
                        names = result.leftOutMembers.map { it.displayName },
                        textColor = textColor
                    )
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = visibleTeams >= result.teams.size,
                enter = fadeIn(tween(220))
            ) {
                ResultActionsRow(
                    textColor = textColor,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                context.getString(R.string.team_results_title),
                                formatTeamResultForClipboard(context, result)
                            )
                        )
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                formatTeamResultForClipboard(context, result)
                            )
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.team_results_title)
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.share_results)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LeftOutResultBlock(
    names: List<String>,
    textColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = textColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.left_out_members_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = stringResource(R.string.left_out_members_count, names.size),
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.72f)
            )
            Text(
                text = names.joinToString(" • "),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                ),
                color = textColor
            )
        }
    }
}

@Composable
private fun ResultActionsRow(
    textColor: Color,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.22f))
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.content_copy_24px),
                contentDescription = null,
                tint = textColor
            )
            Text(
                text = "  " + stringResource(R.string.copy_results),
                color = textColor
            )
        }
        FilledTonalButton(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.share_24px),
                contentDescription = null
            )
            Text(text = "  " + stringResource(R.string.share_results))
        }
    }
}

@Composable
private fun TeamResultBlock(
    team: GeneratedTeam,
    splitMode: TeamSplitMode,
    textColor: Color
) {
    val memberFontSize = when {
        team.members.size <= 2 -> 27.sp
        team.members.size <= 4 -> 23.sp
        else -> 19.sp
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = textColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = teamTitle(team.index, splitMode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = stringResource(R.string.team_members_count_value, team.members.size),
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.72f)
            )
            Text(
                text = team.members.joinToString(" • ") { it.displayName },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = memberFontSize,
                    lineHeight = memberFontSize * 1.18f
                ),
                color = textColor
            )
        }
    }
}

@Composable
private fun resultSubtitle(result: TeamGenerationResult): String {
    val totalPeople = result.teams.sumOf { it.members.size } + result.leftOutMembers.size
    val base = when (result.splitMode) {
        TeamSplitMode.TeamCount -> stringResource(
            R.string.team_results_subtitle_teams,
            result.teams.size,
            totalPeople
        )
        TeamSplitMode.GroupSize -> {
            val maxSize = result.teams.maxOfOrNull { it.members.size } ?: 0
            stringResource(R.string.team_results_subtitle_groups, result.teams.size, maxSize)
        }
    }
    return if (result.leftOutMembers.isEmpty()) {
        base
    } else {
        base + " • " + stringResource(R.string.left_out_members_count, result.leftOutMembers.size)
    }
}

private fun formatTeamResultForClipboard(
    context: Context,
    result: TeamGenerationResult
): String {
    val teamText = result.teams.joinToString(separator = "\n\n") { team ->
        buildString {
            append(teamTitle(context, team.index, result.splitMode))
            append('\n')
            append(team.members.joinToString(", ") { it.displayName })
        }
    }
    if (result.leftOutMembers.isEmpty()) return teamText
    return buildString {
        append(teamText)
        append("\n\n")
        append(context.getString(R.string.left_out_members_title))
        append('\n')
        append(result.leftOutMembers.joinToString(", ") { it.displayName })
    }
}
