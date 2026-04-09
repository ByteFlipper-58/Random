package com.byteflipper.random.ui.presets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHostState
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat
import com.byteflipper.random.data.preset.transfer.PresetTransferPayload
import kotlinx.coroutines.CoroutineScope

internal data class PresetsTransferLaunchers(
    val importFile: () -> Unit,
    val exportPayload: (PresetTransferPayload, Boolean) -> Unit
)

@Composable
internal fun rememberPresetsTransferLaunchers(
    controller: PresetsTransferController,
    viewModel: PresetsViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
): PresetsTransferLaunchers {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launchPresetImport(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onParsed = { controller.importPreview = it }
            ) {
                viewModel.parseImportFromUri(uri)
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        handleExportResult(
            uri = uri,
            pendingPayload = controller.pendingExportPayload,
            isBundleExport = controller.pendingExportIsBundle,
            onComplete = controller::clearPendingExport,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    val exportTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        handleExportResult(
            uri = uri,
            pendingPayload = controller.pendingExportPayload,
            isBundleExport = controller.pendingExportIsBundle,
            onComplete = controller::clearPendingExport,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        handleExportResult(
            uri = uri,
            pendingPayload = controller.pendingExportPayload,
            isBundleExport = controller.pendingExportIsBundle,
            onComplete = controller::clearPendingExport,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }

    return PresetsTransferLaunchers(
        importFile = {
            importLauncher.launch(arrayOf("application/json", "text/plain", "text/csv", "*/*"))
        },
        exportPayload = { payload, isBundle ->
            controller.prepareExport(payload, isBundle)
            when (payload.format) {
                PresetTransferFormat.Csv -> exportCsvLauncher.launch(payload.fileName)
                PresetTransferFormat.Json -> exportJsonLauncher.launch(payload.fileName)
                PresetTransferFormat.Txt -> exportTextLauncher.launch(payload.fileName)
            }
        }
    )
}
