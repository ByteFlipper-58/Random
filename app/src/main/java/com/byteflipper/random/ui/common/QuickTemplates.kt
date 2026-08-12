package com.byteflipper.random.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R

/** A ready-made set of items: its name and its contents. */
data class QuickTemplate(
    val name: String,
    val items: List<String>
)

/** Quick templates shared by the wheel and the list. */
@Composable
fun rememberQuickTemplates(): List<QuickTemplate> = listOf(
    QuickTemplate(
        name = stringResource(R.string.wheel_template_yes_no),
        items = listOf(
            stringResource(R.string.wheel_yes),
            stringResource(R.string.wheel_no)
        )
    ),
    QuickTemplate(
        name = stringResource(R.string.wheel_template_days),
        items = listOf(
            stringResource(R.string.wheel_monday),
            stringResource(R.string.wheel_tuesday),
            stringResource(R.string.wheel_wednesday),
            stringResource(R.string.wheel_thursday),
            stringResource(R.string.wheel_friday),
            stringResource(R.string.wheel_saturday),
            stringResource(R.string.wheel_sunday)
        )
    ),
    QuickTemplate(
        name = stringResource(R.string.wheel_template_colors),
        items = listOf(
            stringResource(R.string.wheel_red),
            stringResource(R.string.wheel_blue),
            stringResource(R.string.wheel_green),
            stringResource(R.string.wheel_yellow),
            stringResource(R.string.wheel_orange),
            stringResource(R.string.wheel_purple)
        )
    )
)
