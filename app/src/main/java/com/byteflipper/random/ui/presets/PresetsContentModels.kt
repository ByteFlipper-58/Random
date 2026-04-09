package com.byteflipper.random.ui.presets

import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset

internal data class PresetSection(
    val titleRes: Int?,
    val presets: List<ListPreset>
)

internal const val SHARE_COPY_MAX_CHARS = 4_096

internal sealed class FormatSelectionTarget {
    abstract val presets: List<ListPreset>
    abstract val titleRes: Int

    data class Export(
        override val presets: List<ListPreset>,
        override val titleRes: Int
    ) : FormatSelectionTarget()

    data class Share(
        override val presets: List<ListPreset>,
        override val titleRes: Int
    ) : FormatSelectionTarget()

    data class Copy(
        override val presets: List<ListPreset>,
        override val titleRes: Int = R.string.copy_format_title
    ) : FormatSelectionTarget()
}
