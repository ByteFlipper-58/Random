package com.byteflipper.random.data.preset.transfer

import kotlinx.serialization.Serializable
import java.util.Locale

const val PRESET_TRANSFER_FORMAT_VERSION: Int = 1
const val PRESET_KIND_LIST: String = "list"
const val PRESET_KIND_BUNDLE: String = "bundle"
const val PRESET_TRANSFER_MAX_ITEM_COUNT: Int = 512_000
const val PRESET_TRANSFER_MAX_BUNDLE_COUNT: Int = 4_096

enum class PresetTransferFormat(
    val mimeType: String,
    val singleFileSuffix: String,
    val bundleFileSuffix: String
) {
    Json(
        mimeType = "application/json",
        singleFileSuffix = ".random-preset.json",
        bundleFileSuffix = ".random-presets.json"
    ),
    Txt(
        mimeType = "text/plain",
        singleFileSuffix = ".random-preset.txt",
        bundleFileSuffix = ".random-presets.txt"
    ),
    Csv(
        mimeType = "text/csv",
        singleFileSuffix = ".random-preset.csv",
        bundleFileSuffix = ".random-presets.csv"
    );

    companion object {
        fun fromFileName(fileName: String?): PresetTransferFormat? {
            val normalized = fileName?.trim()?.lowercase(Locale.ROOT).orEmpty()
            return when {
                normalized.endsWith(".json") -> Json
                normalized.endsWith(".txt") -> Txt
                normalized.endsWith(".csv") -> Csv
                else -> null
            }
        }
    }
}

@Serializable
data class ExportedListPreset(
    val name: String,
    val items: List<String>
)

@Serializable
data class BundledPresetEntry(
    val type: String,
    val name: String,
    val items: List<String>
)

@Serializable
data class SinglePresetTransferFile(
    val formatVersion: Int,
    val kind: String,
    val preset: ExportedListPreset
)

@Serializable
data class PresetBundleTransferFile(
    val formatVersion: Int,
    val kind: String,
    val presets: List<BundledPresetEntry>
)

data class ImportedListPreset(
    val name: String,
    val items: List<String>
)

enum class PresetImportMode {
    Copy,
    ReplaceMatching
}

data class PresetTransferPayload(
    val format: PresetTransferFormat,
    val fileName: String,
    val mimeType: String,
    val content: String,
    val isBundle: Boolean = false,
    val sourcePresetIds: List<Long> = emptyList()
)

enum class PresetImportIssueReason {
    InvalidEntry,
    MissingName,
    MissingItems,
    UnsupportedPresetType,
    TooManyItems
}

data class PresetImportIssue(
    val reason: PresetImportIssueReason,
    val position: Int? = null,
    val name: String? = null
)

data class ParsedPresetImport(
    val presets: List<ImportedListPreset>,
    val issues: List<PresetImportIssue> = emptyList(),
    val isBundle: Boolean = false,
    val format: PresetTransferFormat,
    val sourceLabel: String? = null
)

data class PresetImportResult(
    val importedCount: Int,
    val skippedCount: Int,
    val issues: List<PresetImportIssue> = emptyList(),
    val isBundle: Boolean = false,
    val format: PresetTransferFormat
)
