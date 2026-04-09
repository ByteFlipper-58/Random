package com.byteflipper.random.data.preset.transfer

import com.byteflipper.random.data.preset.ListPreset
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// JSON is the canonical, lossless transfer format for presets and bundles.
class PresetTransferCodec(
    private val maxItemCount: Int = PRESET_TRANSFER_MAX_ITEM_COUNT,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
    }
) {

    fun exportPreset(preset: ListPreset): String {
        val payload = SinglePresetTransferFile(
            formatVersion = PRESET_TRANSFER_FORMAT_VERSION,
            kind = PRESET_KIND_LIST,
            preset = ExportedListPreset(
                name = preset.name,
                items = preset.items
            )
        )
        return json.encodeToString(SinglePresetTransferFile.serializer(), payload)
    }

    fun exportBundle(presets: List<ListPreset>): String {
        if (presets.isEmpty()) {
            throw PresetTransferException.NoPresets
        }
        if (presets.size > PRESET_TRANSFER_MAX_BUNDLE_COUNT) {
            throw PresetTransferException.TooManyPresets
        }

        val payload = PresetBundleTransferFile(
            formatVersion = PRESET_TRANSFER_FORMAT_VERSION,
            kind = PRESET_KIND_BUNDLE,
            presets = presets.map { preset ->
                BundledPresetEntry(
                    type = PRESET_KIND_LIST,
                    name = preset.name,
                    items = preset.items
                )
            }
        )
        return json.encodeToString(PresetBundleTransferFile.serializer(), payload)
    }

    fun importFromJson(input: String): ParsedPresetImport {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw PresetTransferException.EmptyPayload
        }

        val element = try {
            json.parseToJsonElement(trimmed)
        } catch (_: SerializationException) {
            throw PresetTransferException.InvalidJson
        } catch (_: IllegalArgumentException) {
            throw PresetTransferException.InvalidJson
        }

        val root = try {
            element.jsonObject
        } catch (_: IllegalArgumentException) {
            throw PresetTransferException.InvalidJson
        }
        val formatVersion = root["formatVersion"]?.jsonPrimitive?.intOrNull
            ?: throw PresetTransferException.InvalidJson
        if (formatVersion != PRESET_TRANSFER_FORMAT_VERSION) {
            throw PresetTransferException.UnsupportedVersion(formatVersion)
        }

        val kind = root["kind"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        return when (kind) {
            PRESET_KIND_LIST -> {
                val payload = try {
                    json.decodeFromJsonElement(SinglePresetTransferFile.serializer(), element)
                } catch (_: SerializationException) {
                    throw PresetTransferException.InvalidJson
                } catch (_: IllegalArgumentException) {
                    throw PresetTransferException.InvalidJson
                }
                ParsedPresetImport(
                    presets = listOf(
                        ImportedListPreset(
                            name = payload.preset.name,
                            items = payload.preset.items
                        )
                    ).map(::sanitizeImportedPreset),
                    isBundle = false,
                    format = PresetTransferFormat.Json
                )
            }

            PRESET_KIND_BUNDLE -> importBundleFromJson(root["presets"], format = PresetTransferFormat.Json)

            else -> throw PresetTransferException.UnsupportedKind(kind)
        }
    }

    private fun importBundleFromJson(
        presetsElement: JsonElement?,
        format: PresetTransferFormat
    ): ParsedPresetImport {
        val presetsArray = try {
            presetsElement?.jsonArray
        } catch (_: IllegalArgumentException) {
            null
        } ?: throw PresetTransferException.InvalidJson

        if (presetsArray.isEmpty()) {
            throw PresetTransferException.NoPresets
        }
        if (presetsArray.size > PRESET_TRANSFER_MAX_BUNDLE_COUNT) {
            throw PresetTransferException.TooManyPresets
        }

        val issues = mutableListOf<PresetImportIssue>()
        val presets = presetsArray.mapIndexedNotNull { index, entry ->
            parseBundledEntry(entry, index + 1, issues)
        }

        return ParsedPresetImport(
            presets = presets,
            issues = issues,
            isBundle = true,
            format = format
        )
    }

    private fun parseBundledEntry(
        entry: JsonElement,
        position: Int,
        issues: MutableList<PresetImportIssue>
    ): ImportedListPreset? {
        val objectEntry = try {
            entry.jsonObject
        } catch (_: IllegalArgumentException) {
            issues += PresetImportIssue(
                reason = PresetImportIssueReason.InvalidEntry,
                position = position
            )
            return null
        }

        val type = objectEntry["type"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (type != PRESET_KIND_LIST) {
            issues += PresetImportIssue(
                reason = PresetImportIssueReason.UnsupportedPresetType,
                position = position
            )
            return null
        }

        val name = objectEntry["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val items = try {
            val rawItems = objectEntry["items"]?.jsonArray
            rawItems?.mapNotNull { item ->
                item.jsonPrimitive.contentOrNull
            }.orEmpty()
        } catch (_: IllegalArgumentException) {
            issues += PresetImportIssue(
                reason = PresetImportIssueReason.InvalidEntry,
                position = position,
                name = name.ifBlank { null }
            )
            return null
        }

        return try {
            sanitizeImportedPreset(
                ImportedListPreset(
                    name = name,
                    items = items
                )
            )
        } catch (error: PresetTransferException) {
            issues += PresetImportIssue(
                reason = error.toIssueReason(),
                position = position,
                name = name.ifBlank { null }
            )
            null
        }
    }

    private fun sanitizeImportedPreset(preset: ImportedListPreset): ImportedListPreset {
        val name = preset.name.trim()
        val items = preset.items.map { it.trim() }.filter { it.isNotEmpty() }

        if (name.isEmpty()) {
            throw PresetTransferException.MissingTextName
        }
        if (items.isEmpty()) {
            throw PresetTransferException.MissingTextItems
        }
        if (items.size > maxItemCount) {
            throw PresetTransferException.TooManyItems
        }

        return ImportedListPreset(
            name = name,
            items = items
        )
    }

    private fun PresetTransferException.toIssueReason(): PresetImportIssueReason {
        return when (this) {
            PresetTransferException.MissingTextName -> PresetImportIssueReason.MissingName
            PresetTransferException.MissingTextItems -> PresetImportIssueReason.MissingItems
            PresetTransferException.TooManyItems -> PresetImportIssueReason.TooManyItems
            else -> PresetImportIssueReason.InvalidEntry
        }
    }
}

sealed class PresetTransferException : IllegalArgumentException() {
    data object InvalidCsv : PresetTransferException()
    data object InvalidJson : PresetTransferException()
    data object EmptyPayload : PresetTransferException()
    data object EmptyText : PresetTransferException()
    data object NoPresets : PresetTransferException()
    data object MissingTextName : PresetTransferException()
    data object MissingTextItems : PresetTransferException()
    data object FileTooLarge : PresetTransferException()
    data object TooManyItems : PresetTransferException()
    data object TooManyPresets : PresetTransferException()
    data object UnsupportedEncoding : PresetTransferException()
    data class UnsupportedFileFormat(val fileName: String?) : PresetTransferException()
    data class UnsupportedVersion(val version: Int) : PresetTransferException()
    data class UnsupportedKind(val kind: String) : PresetTransferException()
}
