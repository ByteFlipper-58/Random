package com.byteflipper.random.ui.teams.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.person.Person
import com.byteflipper.random.data.person.PersonGender
import com.byteflipper.random.data.person.formatBirth
import com.byteflipper.random.data.team.TeamSplitMode

@Composable
fun buildPersonSubtitle(person: Person): String {
    val genderText = when (person.gender) {
        PersonGender.Unspecified -> stringResource(R.string.person_gender_unspecified)
        PersonGender.Male -> stringResource(R.string.person_gender_male)
        PersonGender.Female -> stringResource(R.string.person_gender_female)
    }
    val parts = buildList {
        add(genderText)
        person.formatBirth(LocalLocale.current.platformLocale)?.let { add(it) }
    }
    return parts.joinToString(" • ")
}

fun contrastColorFor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
}

@Composable
fun teamTitle(
    index: Int,
    splitMode: TeamSplitMode
): String = when (splitMode) {
    TeamSplitMode.TeamCount -> stringResource(R.string.team_label, index + 1)
    TeamSplitMode.GroupSize -> stringResource(R.string.group_label, index + 1)
}

fun teamTitle(
    context: Context,
    index: Int,
    splitMode: TeamSplitMode
): String = when (splitMode) {
    TeamSplitMode.TeamCount -> context.getString(R.string.team_label, index + 1)
    TeamSplitMode.GroupSize -> context.getString(R.string.group_label, index + 1)
}
