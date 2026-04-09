package com.byteflipper.random.ui.presets

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.transfer.ParsedPresetImport
import com.byteflipper.random.data.preset.transfer.PresetTransferPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun handleExportResult(
    uri: Uri?,
    pendingPayload: PresetTransferPayload?,
    isBundleExport: Boolean,
    onComplete: () -> Unit,
    viewModel: PresetsViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val exportPayload = pendingPayload
    onComplete()

    if (uri != null && exportPayload != null) {
        scope.launch {
            try {
                val message = if (isBundleExport) {
                    viewModel.writeBundleExportToUri(exportPayload, uri)
                } else {
                    viewModel.writeExportToUri(exportPayload, uri)
                }
                snackbarHostState.showSnackbar(message)
            } catch (error: IllegalStateException) {
                snackbarHostState.showSnackbar(error.message.orEmpty())
            }
        }
    }
}

internal fun buildSections(uiState: PresetsUiState): List<PresetSection> {
    if (uiState.presets.isEmpty()) return emptyList()

    return when (uiState.filter) {
        PresetFilter.All -> {
            val pinned = uiState.presets.filter { it.isPinned }
            val others = uiState.presets.filterNot { it.isPinned }
            buildList {
                if (pinned.isNotEmpty()) {
                    add(PresetSection(R.string.pinned, pinned))
                }
                if (others.isNotEmpty()) {
                    add(
                        PresetSection(
                            titleRes = if (pinned.isNotEmpty()) R.string.more_presets else null,
                            presets = others
                        )
                    )
                }
            }
        }

        PresetFilter.Recent -> listOf(PresetSection(null, uiState.presets))
        PresetFilter.MostUsed -> listOf(PresetSection(null, uiState.presets))
    }
}

internal fun CoroutineScope.launchPresetImport(
    viewModel: PresetsViewModel,
    snackbarHostState: SnackbarHostState,
    onParsed: (ParsedPresetImport) -> Unit,
    block: suspend () -> ParsedPresetImport
) {
    launch {
        try {
            onParsed(block())
        } catch (error: IllegalStateException) {
            snackbarHostState.showSnackbar(error.message.orEmpty())
        }
    }
}
