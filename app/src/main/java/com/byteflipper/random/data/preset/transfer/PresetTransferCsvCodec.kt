package com.byteflipper.random.data.preset.transfer

import com.byteflipper.random.data.preset.ListPreset
import java.io.Writer

// CSV uses the canonical columns `name,item`. Repeated names form a bundle.
// CSV is lossless for commas/newlines when fields are quoted correctly.
class PresetTransferCsvCodec(
    private val maxItemCount: Int = PRESET_TRANSFER_MAX_ITEM_COUNT
) {

    fun exportPreset(preset: ListPreset): String {
        return buildString {
            writeHeader(this)
            preset.items.forEach { item ->
                writeRecord(this, preset.name, item)
            }
        }
    }

    fun exportBundle(presets: List<ListPreset>): String {
        validatePresetCount(presets.size)
        return buildString {
            writeHeader(this)
            presets.forEach { preset ->
                preset.items.forEach { item ->
                    writeRecord(this, preset.name, item)
                }
            }
        }
    }

    fun writeBundle(writer: Writer, presets: List<ListPreset>) {
        validatePresetCount(presets.size)
        writeHeader(writer)
        presets.forEach { preset ->
            preset.items.forEach { item ->
                writeRecord(writer, preset.name, item)
            }
        }
    }

    fun importFromCsv(
        input: String,
        fallbackName: String?,
        defaultName: String
    ): ParsedPresetImport {
        val records = parseRecords(input)
        if (records.isEmpty()) {
            throw PresetTransferException.EmptyText
        }

        val hasHeader = records.first().matchesHeader()
        val dataRecords = if (hasHeader) records.drop(1) else records
        if (dataRecords.isEmpty()) {
            throw PresetTransferException.NoPresets
        }

        val nonEmptyNames = dataRecords
            .mapNotNull { record -> record.primaryNameOrNull() }
            .toSet()
        val fallbackResolvedName = resolveFallbackName(fallbackName, defaultName)
        val canUseFallbackName = nonEmptyNames.size <= 1

        val issues = mutableListOf<PresetImportIssue>()
        val groupedItems = linkedMapOf<String, MutableList<String>>()

        dataRecords.forEach { record ->
            val fields = record.fields
            if (fields.isEmpty() || fields.all { it.isBlank() }) {
                return@forEach
            }

            val rawName = fields.getOrNull(0).orEmpty().trim()
            val rawItem = when {
                fields.size >= 2 -> fields.drop(1).joinToString(",")
                else -> ""
            }.trim()

            if (rawItem.isEmpty()) {
                issues += PresetImportIssue(
                    reason = if (fields.size < 2) PresetImportIssueReason.InvalidEntry else PresetImportIssueReason.MissingItems,
                    position = record.rowNumber,
                    name = rawName.ifBlank { null }
                )
                return@forEach
            }

            val resolvedName = when {
                rawName.isNotEmpty() -> rawName
                canUseFallbackName -> fallbackResolvedName
                else -> null
            }

            if (resolvedName.isNullOrEmpty()) {
                issues += PresetImportIssue(
                    reason = PresetImportIssueReason.MissingName,
                    position = record.rowNumber
                )
                return@forEach
            }

            val items = groupedItems.getOrPut(resolvedName) { mutableListOf() }
            items += rawItem
        }

        val presets = groupedItems.mapNotNull { (name, items) ->
            if (items.size > maxItemCount) {
                issues += PresetImportIssue(
                    reason = PresetImportIssueReason.TooManyItems,
                    name = name
                )
                null
            } else {
                ImportedListPreset(
                    name = name,
                    items = items
                )
            }
        }

        return ParsedPresetImport(
            presets = presets,
            issues = issues,
            isBundle = presets.size > 1 || nonEmptyNames.size > 1,
            format = PresetTransferFormat.Csv
        )
    }

    private fun parseRecords(input: String): List<CsvRecord> {
        val normalized = input
            .replace("\uFEFF", "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        if (normalized.isBlank()) {
            return emptyList()
        }

        val records = mutableListOf<CsvRecord>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var rowNumber = 1
        var recordRowNumber = 1
        var index = 0

        while (index < normalized.length) {
            val char = normalized[index]
            when {
                inQuotes && char == '"' -> {
                    val nextChar = normalized.getOrNull(index + 1)
                    if (nextChar == '"') {
                        field.append('"')
                        index += 1
                    } else {
                        inQuotes = false
                    }
                }
                !inQuotes && char == '"' -> {
                    if (field.isNotEmpty()) {
                        throw PresetTransferException.InvalidCsv
                    }
                    inQuotes = true
                }
                !inQuotes && char == ',' -> {
                    fields += field.toString()
                    field.clear()
                }
                !inQuotes && char == '\n' -> {
                    fields += field.toString()
                    field.clear()
                    if (fields.any { it.isNotEmpty() }) {
                        records += CsvRecord(
                            rowNumber = recordRowNumber,
                            fields = fields.toList()
                        )
                    }
                    fields.clear()
                    rowNumber += 1
                    recordRowNumber = rowNumber
                }
                else -> {
                    field.append(char)
                    if (char == '\n') {
                        rowNumber += 1
                    }
                }
            }
            index += 1
        }

        if (inQuotes) {
            throw PresetTransferException.InvalidCsv
        }

        fields += field.toString()
        if (fields.any { it.isNotEmpty() }) {
            records += CsvRecord(
                rowNumber = recordRowNumber,
                fields = fields.toList()
            )
        }

        return records
    }

    private fun writeHeader(appendable: Appendable) {
        appendable.appendLine("name,item")
    }

    private fun writeRecord(appendable: Appendable, name: String, item: String) {
        appendable
            .append(escapeCsv(name))
            .append(',')
            .append(escapeCsv(item))
            .appendLine()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) {
            return value
        }
        return buildString {
            append('"')
            value.forEach { char ->
                if (char == '"') {
                    append("\"\"")
                } else {
                    append(char)
                }
            }
            append('"')
        }
    }

    private fun resolveFallbackName(
        fallbackName: String?,
        defaultName: String
    ): String {
        val resolved = fallbackName
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.trim()
            .orEmpty()

        return resolved.ifEmpty { defaultName }
    }

    private fun validatePresetCount(count: Int) {
        if (count == 0) {
            throw PresetTransferException.NoPresets
        }
        if (count > PRESET_TRANSFER_MAX_BUNDLE_COUNT) {
            throw PresetTransferException.TooManyPresets
        }
    }

    private data class CsvRecord(
        val rowNumber: Int,
        val fields: List<String>
    ) {
        fun matchesHeader(): Boolean {
            if (fields.size < 2) return false
            return fields[0].trim().equals("name", ignoreCase = true) &&
                fields[1].trim().equals("item", ignoreCase = true)
        }

        fun primaryNameOrNull(): String? {
            return fields.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
