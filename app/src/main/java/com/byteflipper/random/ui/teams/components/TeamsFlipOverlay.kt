package com.byteflipper.random.ui.teams.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
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
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val maxCardWidth = (screenWidthDp - 32.dp).coerceAtLeast(240.dp)
    val maxCardHeight = (screenHeightDp - 160.dp).coerceIn(320.dp, 580.dp)

    val teams = result?.teams.orEmpty()
    val teamsCount = teams.size
    val leftOutCount = result?.leftOutMembers?.size ?: 0

    val minHeight = 320.dp.coerceAtMost(maxCardHeight)
    val initialCardWidth = 304.dp.coerceAtMost(maxCardWidth)
    val initialCardHeight = 320.dp.coerceIn(minHeight, maxCardHeight)

    val targetCardWidth = if (isGenerating || result == null) {
        initialCardWidth
    } else {
        when {
            teamsCount <= 2 -> 304.dp
            teamsCount <= 4 -> 324.dp
            teamsCount <= 6 -> 338.dp
            else -> 348.dp
        }.coerceAtMost(maxCardWidth)
    }

    val targetCardHeight = remember(result, isGenerating, maxCardHeight) {
        if (isGenerating || result == null) {
            initialCardHeight
        } else {
            var contentHeight = 62.dp + 104.dp + 26.dp
            teams.forEach { team ->
                val memberCount = team.members.size
                val maxNameLen = team.members.maxOfOrNull { it.displayName.length } ?: 1
                val estLines = when {
                    memberCount <= 1 -> 1
                    memberCount <= 2 && maxNameLen <= 12 -> 1
                    memberCount <= 3 && maxNameLen <= 16 -> 2
                    memberCount <= 4 -> 2
                    memberCount <= 6 -> 3
                    else -> (memberCount + 1) / 2
                }
                val blockHeight = 40.dp + (estLines * 22).dp + 20.dp + 10.dp
                contentHeight += blockHeight
            }
            if (leftOutCount > 0) {
                val estLines = maxOf(1, (leftOutCount + 1) / 2)
                val leftOutHeight = 40.dp + (estLines * 20).dp + 20.dp + 10.dp
                contentHeight += leftOutHeight
            }
            contentHeight.coerceIn(minHeight, maxCardHeight)
        }
    }

    val animatedCardWidth by animateDpAsState(
        targetValue = targetCardWidth,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = if (isGenerating) Spring.StiffnessMedium else Spring.StiffnessLow
        ),
        label = "teams_overlay_width"
    )
    val animatedCardHeight by animateDpAsState(
        targetValue = targetCardHeight,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = if (isGenerating) Spring.StiffnessMedium else Spring.StiffnessLow
        ),
        label = "teams_overlay_height"
    )

    fun copyResultsToClipboard(generatedResult: TeamGenerationResult) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.team_results_title),
                formatTeamResultForClipboard(context, generatedResult)
            )
        )
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard))
        }
    }

    fun shareResults(generatedResult: TeamGenerationResult) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                formatTeamResultForClipboard(context, generatedResult)
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
            copyResultsToClipboard(generatedResult)
        },
        frontContent = {
            CardContentTheme {
                TeamsResultsDisplay(
                    result = result,
                    cardColor = animatedColor.value,
                    onCopy = { result?.let(::copyResultsToClipboard) },
                    onShare = { result?.let(::shareResults) }
                )
            }
        },
        backContent = {
            CardContentTheme {
                TeamsResultsDisplay(
                    result = result,
                    cardColor = animatedColor.value,
                    onCopy = { result?.let(::copyResultsToClipboard) },
                    onShare = { result?.let(::shareResults) }
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
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    if (result == null) return

    val textColor = contrastColorFor(cardColor)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentPadding = PaddingValues(bottom = 6.dp),
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
            TeamResultBlock(
                team = team,
                splitMode = result.splitMode,
                textColor = textColor
            )
        }
        if (result.leftOutMembers.isNotEmpty()) {
            item {
                LeftOutResultBlock(
                    names = result.leftOutMembers.map { it.displayName },
                    textColor = textColor
                )
            }
        }
        item {
            ResultActionsRow(
                textColor = textColor,
                onCopy = onCopy,
                onShare = onShare
            )
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
        shape = RoundedCornerShape(18.dp),
        color = textColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    fontSize = 16.sp,
                    lineHeight = 20.sp
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
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = textColor.copy(alpha = 0.18f),
                contentColor = textColor
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
        ) {
            Icon(
                painter = painterResource(R.drawable.content_copy_24px),
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.copy_results),
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = textColor.copy(alpha = 0.12f),
                contentColor = textColor
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.18f))
        ) {
            Icon(
                painter = painterResource(R.drawable.share_24px),
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.share_results),
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TeamResultBlock(
    team: GeneratedTeam,
    splitMode: TeamSplitMode,
    textColor: Color
) {
    val maxMemberNameLen = team.members.maxOfOrNull { it.displayName.length } ?: 1
    val memberFontSize = when {
        team.members.size <= 2 && maxMemberNameLen <= 12 -> 20.sp
        team.members.size <= 3 && maxMemberNameLen <= 18 -> 18.sp
        team.members.size <= 5 -> 16.sp
        else -> 15.sp
    }
    val lineHeight = memberFontSize * 1.25f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = textColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    lineHeight = lineHeight
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
