package com.byteflipper.random.ui.presets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.preset.transfer.ParsedPresetImport
import com.byteflipper.random.data.preset.transfer.PresetImportMode
import com.byteflipper.random.data.preset.transfer.PresetImportIssue
import com.byteflipper.random.data.preset.transfer.PresetImportIssueReason
import com.byteflipper.random.data.preset.transfer.PresetTransferException
import com.byteflipper.random.data.preset.transfer.PresetTransferFormat
import com.byteflipper.random.data.preset.transfer.PresetTransferPayload
import com.byteflipper.random.data.preset.transfer.PresetTransferService
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale

enum class PresetFilter {
    All,
    Recent,
    MostUsed
}

data class PresetsUiState(
    val filter: PresetFilter = PresetFilter.All,
    val sortAscending: Boolean = true,
    val availablePresets: List<ListPreset> = emptyList(),
    val presets: List<ListPreset> = emptyList(),
    val hasAnyPresets: Boolean = false,
    val lastUsedPresetId: Long? = null,
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0
)

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val listPresetRepository: ListPresetRepository,
    settingsRepository: SettingsRepository,
    private val presetTransferService: PresetTransferService,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private companion object {
        const val RECENT_PRESETS_LIMIT = 12
    }

    private data class ListViewportState(
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0
    )

    private val filter = MutableStateFlow(PresetFilter.All)
    private val sortAscending = MutableStateFlow(true)
    private val listViewportState = MutableStateFlow(ListViewportState())

    val settings: StateFlow<Settings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    val lastTransferFormat: StateFlow<PresetTransferFormat> = settingsRepository.lastPresetTransferFormatFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PresetTransferFormat.Json
    )

    private val settingsRepository = settingsRepository

    private val contentState = combine(
        listPresetRepository.observeAll(),
        filter,
        sortAscending
    ) { presets, currentFilter, currentSortAscending ->
        val recentPresetIds = presets
            .sortedByDescending(::activityAt)
            .take(RECENT_PRESETS_LIMIT)
            .mapTo(mutableSetOf()) { it.id }

        val availablePresets = presets
            .filter { preset ->
                when (currentFilter) {
                    PresetFilter.All -> true
                    PresetFilter.Recent -> preset.id in recentPresetIds
                    PresetFilter.MostUsed -> preset.useCount > 0
                }
            }
            .sortedWith(comparatorFor(currentFilter, currentSortAscending))

        val lastUsedPresetId = presets
            .filter { it.lastUsedAt != null }
            .maxByOrNull { it.lastUsedAt ?: Long.MIN_VALUE }
            ?.id

        PresetsUiState(
            filter = currentFilter,
            sortAscending = currentSortAscending,
            availablePresets = availablePresets,
            presets = availablePresets,
            hasAnyPresets = presets.isNotEmpty(),
            lastUsedPresetId = lastUsedPresetId
        )
    }

    val uiState: StateFlow<PresetsUiState> = combine(
        contentState,
        listViewportState
    ) { state, viewport ->
        state.copy(
            firstVisibleItemIndex = viewport.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = viewport.firstVisibleItemScrollOffset
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PresetsUiState()
    )

    fun updateFilter(value: PresetFilter) {
        filter.update { value }
    }

    fun toggleSortOrder() {
        sortAscending.update { !it }
    }

    fun updateListViewport(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        listViewportState.update { current ->
            if (
                current.firstVisibleItemIndex == firstVisibleItemIndex &&
                current.firstVisibleItemScrollOffset == firstVisibleItemScrollOffset
            ) {
                current
            } else {
                current.copy(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
                )
            }
        }
    }

    fun renamePreset(preset: ListPreset, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return

        viewModelScope.launch {
            listPresetRepository.upsert(
                preset.copy(
                    name = trimmedName,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePreset(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.delete(preset)
        }
    }

    fun restorePreset(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.restore(preset)
        }
    }

    suspend fun duplicatePreset(preset: ListPreset, copyName: String): ListPreset {
        return listPresetRepository.duplicate(preset, copyName)
    }

    fun togglePinned(preset: ListPreset) {
        viewModelScope.launch {
            listPresetRepository.setPinned(preset.id, !preset.isPinned)
        }
    }

    suspend fun prepareExportPayload(
        presets: List<ListPreset>,
        format: PresetTransferFormat
    ): PresetTransferPayload {
        settingsRepository.setLastPresetTransferFormat(format)
        return runTransferResult {
            if (presets.size == 1) {
                presetTransferService.exportPreset(presets.first(), format)
            } else {
                presetTransferService.exportPresets(presets, format)
            }
        }
    }

    suspend fun writeExportToUri(payload: PresetTransferPayload, uri: Uri): String {
        return runTransferAction(R.string.preset_exported) {
            presetTransferService.writeExportToUri(payload, uri)
        }
    }

    suspend fun writeBundleExportToUri(payload: PresetTransferPayload, uri: Uri): String {
        return runTransferAction(R.string.presets_exported) {
            presetTransferService.writeExportToUri(payload, uri)
        }
    }

    suspend fun parseImportFromUri(uri: Uri): ParsedPresetImport {
        return runTransferResult {
            presetTransferService.parseImportFromUri(uri)
        }
    }

    suspend fun parseImportFromClipboard(text: String): ParsedPresetImport {
        return runTransferResult {
            presetTransferService.parseImportFromClipboard(text)
        }
    }

    suspend fun commitImport(parsedImport: ParsedPresetImport, mode: PresetImportMode): String {
        return runTransferResult {
            val result = presetTransferService.commitImport(parsedImport, mode)
            buildImportMessage(importedCount = result.importedCount, issues = result.issues)
        }
    }

    suspend fun createShareIntent(
        payload: PresetTransferPayload,
        subject: String
    ): Intent {
        return runTransferResult {
            settingsRepository.setLastPresetTransferFormat(payload.format)
            presetTransferService.createShareIntent(payload, subject)
        }
    }

    suspend fun mergePresets(
        presets: List<ListPreset>,
        mergedName: String
    ): ListPreset {
        return listPresetRepository.mergePresets(presets, mergedName)
    }

    private fun comparatorFor(
        filter: PresetFilter,
        ascending: Boolean
    ): Comparator<ListPreset> {
        val locale = Locale.getDefault()

        val comparator = when (filter) {
            PresetFilter.Recent -> compareBy<ListPreset>(::activityAt)
                .thenBy { it.name.lowercase(locale) }

            PresetFilter.MostUsed -> compareBy<ListPreset> { it.useCount }
                .thenBy(::activityAt)
                .thenBy { it.name.lowercase(locale) }

            PresetFilter.All -> compareBy<ListPreset> { it.name.lowercase(locale) }
                .thenByDescending(::activityAt)
        }

        return if (ascending) comparator else comparator.reversed()
    }

    private fun activityAt(preset: ListPreset): Long {
        return preset.lastUsedAt ?: preset.updatedAt
    }

    private suspend fun runTransferAction(
        successMessageRes: Int,
        block: suspend () -> Unit
    ): String {
        return try {
            block()
            appContext.getString(successMessageRes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw IllegalStateException(transferErrorMessage(error), error)
        }
    }

    private suspend fun <T> runTransferResult(block: suspend () -> T): T {
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw IllegalStateException(transferErrorMessage(error), error)
        }
    }

    private fun transferErrorMessage(error: Throwable): String {
        return when (error) {
            is PresetTransferException.EmptyPayload -> appContext.getString(R.string.preset_transfer_error_empty)
            is PresetTransferException.EmptyText -> appContext.getString(R.string.preset_transfer_error_empty)
            is PresetTransferException.FileTooLarge -> appContext.getString(R.string.preset_transfer_error_file_too_large)
            is PresetTransferException.InvalidCsv -> appContext.getString(R.string.preset_transfer_error_invalid_csv)
            is PresetTransferException.InvalidJson -> appContext.getString(R.string.preset_transfer_error_invalid_json)
            is PresetTransferException.NoPresets -> appContext.getString(R.string.preset_transfer_error_no_presets)
            is PresetTransferException.MissingTextItems -> appContext.getString(R.string.preset_transfer_error_missing_text_items)
            is PresetTransferException.MissingTextName -> appContext.getString(R.string.preset_transfer_error_missing_text_name)
            is PresetTransferException.TooManyItems -> appContext.getString(R.string.preset_transfer_error_too_many_items)
            is PresetTransferException.TooManyPresets -> appContext.getString(R.string.preset_transfer_error_too_many_presets)
            is PresetTransferException.UnsupportedEncoding -> appContext.getString(R.string.preset_transfer_error_unsupported_encoding)
            is PresetTransferException.UnsupportedFileFormat -> appContext.getString(R.string.preset_transfer_error_unsupported_file_format)
            is PresetTransferException.UnsupportedKind -> appContext.getString(R.string.preset_transfer_error_unsupported_kind)
            is PresetTransferException.UnsupportedVersion -> appContext.getString(R.string.preset_transfer_error_unsupported_version)
            is java.io.IOException -> appContext.getString(R.string.preset_transfer_error_io)
            else -> appContext.getString(R.string.preset_transfer_error_generic)
        }
    }

    private fun buildImportMessage(
        importedCount: Int,
        issues: List<PresetImportIssue>
    ): String {
        if (issues.isEmpty()) {
            return appContext.getString(R.string.preset_imported_count, importedCount)
        }

        val issueSummary = issues
            .groupingBy { it.reason }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .joinToString(separator = ", ") { (reason, count) ->
                appContext.getString(reason.toSummaryStringRes(), count)
            }

        return if (importedCount > 0) {
            appContext.getString(
                R.string.preset_imported_partial_count,
                importedCount,
                issues.size,
                issueSummary
            )
        } else {
            appContext.getString(
                R.string.preset_imported_none_valid,
                issues.size,
                issueSummary
            )
        }
    }

    private fun PresetImportIssueReason.toSummaryStringRes(): Int {
        return when (this) {
            PresetImportIssueReason.InvalidEntry -> R.string.preset_import_issue_invalid_entry
            PresetImportIssueReason.MissingName -> R.string.preset_import_issue_missing_name
            PresetImportIssueReason.MissingItems -> R.string.preset_import_issue_missing_items
            PresetImportIssueReason.UnsupportedPresetType -> R.string.preset_import_issue_unsupported_type
            PresetImportIssueReason.TooManyItems -> R.string.preset_import_issue_too_many_items
        }
    }
}
