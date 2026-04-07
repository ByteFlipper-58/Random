package com.byteflipper.random.ui.presets

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset

@Composable
fun presetMetaText(
    preset: ListPreset
): String {
    val usageText = preset.lastUsedAt?.let {
        stringResource(
            R.string.preset_used_relative,
            DateUtils.getRelativeTimeSpanString(
                it,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        )
    } ?: stringResource(R.string.preset_not_used_yet)

    return listOf(
        stringResource(R.string.preset_items_count, preset.items.size),
        usageText
    ).joinToString(" • ")
}
