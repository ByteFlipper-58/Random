package com.byteflipper.random.data.preset.transfer

import java.io.Writer

// TXT import is tolerant and lossy. Single export stays canonical:
// {name}, blank line, then one item per line. TXT bundles use explicit
// bundle and preset markers so multiple presets can round-trip safely.
class PresetTransferTextCodec(
    private val maxItemCount: Int = PRESET_TRANSFER_MAX_ITEM_COUNT
) {

    fun exportPreset(preset: ImportedListPreset): String {
        return buildString {
            appendLine(preset.name)
            appendLine()
            preset.items.forEachIndexed { index, item ->
                append(item)
                if (index != preset.items.lastIndex) {
                    appendLine()
                }
            }
        }
    }

    fun exportBundle(presets: List<ImportedListPreset>): String {
        if (presets.isEmpty()) {
            throw PresetTransferException.NoPresets
        }
        if (presets.size > PRESET_TRANSFER_MAX_BUNDLE_COUNT) {
            throw PresetTransferException.TooManyPresets
        }

        return buildString {
            writeBundleTo(appendable = this, presets = presets)
        }
    }

    fun writeBundle(writer: Writer, presets: List<ImportedListPreset>) {
        if (presets.isEmpty()) {
            throw PresetTransferException.NoPresets
        }
        if (presets.size > PRESET_TRANSFER_MAX_BUNDLE_COUNT) {
            throw PresetTransferException.TooManyPresets
        }
        writeBundleTo(appendable = writer, presets = presets)
    }

    fun importFromText(
        input: String,
        fallbackName: String?,
        defaultName: String
    ): ParsedPresetImport {
        val normalized = input
            .replace("\uFEFF", "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\t', ' ')
            .trim(::isTrimWhitespace)

        if (normalized.isEmpty()) {
            throw PresetTransferException.EmptyText
        }

        val lines = normalized.lines()
        val nonEmptyLines = lines
            .map(::normalizeLine)
            .filter { it.isNotEmpty() }

        if (nonEmptyLines.isEmpty()) {
            throw PresetTransferException.EmptyText
        }

        return if (looksLikeBundle(nonEmptyLines)) {
            importBundleText(lines)
        } else {
            ParsedPresetImport(
                presets = listOf(importSingleText(lines, nonEmptyLines, fallbackName, defaultName)),
                isBundle = false,
                format = PresetTransferFormat.Txt
            )
        }
    }

    private fun importBundleText(lines: List<String>): ParsedPresetImport {
        val presets = mutableListOf<ImportedListPreset>()
        val issues = mutableListOf<PresetImportIssue>()
        var currentName: String? = null
        val currentItems = mutableListOf<String>()
        var currentPosition = 0
        var skipUntilNextHeader = false
        var reportedLeadingInvalidEntry = false

        fun flushCurrentPreset() {
            val name = currentName ?: return
            if (currentItems.isEmpty()) {
                issues += PresetImportIssue(
                    reason = PresetImportIssueReason.MissingItems,
                    position = currentPosition,
                    name = name
                )
            } else if (currentItems.size > maxItemCount) {
                issues += PresetImportIssue(
                    reason = PresetImportIssueReason.TooManyItems,
                    position = currentPosition,
                    name = name
                )
            } else {
                presets += ImportedListPreset(
                    name = name,
                    items = currentItems.toList()
                )
            }
            currentName = null
            currentItems.clear()
            skipUntilNextHeader = false
        }

        lines.forEach { rawLine ->
            val line = normalizeLine(rawLine)
            when {
                line.isEmpty() || line == BUNDLE_HEADER -> Unit
                line.startsWith(PRESET_HEADER_PREFIX) -> {
                    flushCurrentPreset()
                    currentPosition += 1
                    reportedLeadingInvalidEntry = false
                    val name = line.removePrefix(PRESET_HEADER_PREFIX).trim(::isTrimWhitespace)
                    if (name.isEmpty()) {
                        issues += PresetImportIssue(
                            reason = PresetImportIssueReason.MissingName,
                            position = currentPosition
                        )
                        currentName = null
                        currentItems.clear()
                        skipUntilNextHeader = true
                    } else {
                        currentName = name
                    }
                }
                currentName == null -> {
                    if (!reportedLeadingInvalidEntry) {
                        val position = if (currentPosition == 0) 1 else currentPosition
                        issues += PresetImportIssue(
                            reason = PresetImportIssueReason.InvalidEntry,
                            position = position
                        )
                        reportedLeadingInvalidEntry = true
                    }
                }
                else -> {
                    if (!skipUntilNextHeader) {
                        currentItems += parseLineItems(line)
                    }
                }
            }
        }

        flushCurrentPreset()

        if (presets.isEmpty()) {
            if (issues.isNotEmpty()) {
                return ParsedPresetImport(
                    presets = emptyList(),
                    issues = issues,
                    isBundle = true,
                    format = PresetTransferFormat.Txt
                )
            }
            throw PresetTransferException.NoPresets
        }
        if (presets.size > PRESET_TRANSFER_MAX_BUNDLE_COUNT) {
            throw PresetTransferException.TooManyPresets
        }

        return ParsedPresetImport(
            presets = presets,
            issues = issues,
            isBundle = true,
            format = PresetTransferFormat.Txt
        )
    }

    private fun importSingleText(
        lines: List<String>,
        nonEmptyLines: List<String>,
        fallbackName: String?,
        defaultName: String
    ): ImportedListPreset {
        return if (looksLikeCanonicalSingle(lines)) {
            importCanonicalText(lines, defaultName)
        } else {
            importLooseText(nonEmptyLines, fallbackName, defaultName)
        }
    }

    private fun importCanonicalText(
        lines: List<String>,
        defaultName: String
    ): ImportedListPreset {
        val firstNameIndex = lines.indexOfFirst { normalizeLine(it).isNotEmpty() }
        if (firstNameIndex == -1) {
            throw PresetTransferException.MissingTextName
        }

        val name = normalizeLine(lines[firstNameIndex]).ifEmpty { defaultName }
        val items = parseItems(
            lines = lines.drop(firstNameIndex + 1),
            allowLeadingBlankLines = true
        )

        if (items.isEmpty()) {
            throw PresetTransferException.MissingTextItems
        }

        return ImportedListPreset(
            name = name,
            items = items
        )
    }

    private fun importLooseText(
        lines: List<String>,
        fallbackName: String?,
        defaultName: String
    ): ImportedListPreset {
        val items = parseItems(
            lines = lines,
            allowLeadingBlankLines = false
        )

        if (items.isEmpty()) {
            throw PresetTransferException.MissingTextItems
        }

        return ImportedListPreset(
            name = resolveFallbackName(fallbackName, defaultName),
            items = items
        )
    }

    private fun parseItems(
        lines: List<String>,
        allowLeadingBlankLines: Boolean
    ): List<String> {
        val sourceLines = if (allowLeadingBlankLines) {
            lines.dropWhile { normalizeLine(it).isEmpty() }
        } else {
            lines
        }

        val items = ArrayList<String>()
        sourceLines.forEach { rawLine ->
            val line = normalizeLine(rawLine)
            if (line.isEmpty()) return@forEach

            parseLineItems(line).forEach { token ->
                items += token
                if (items.size > maxItemCount) {
                    throw PresetTransferException.TooManyItems
                }
            }
        }
        return items
    }

    private fun parseLineItems(line: String): List<String> {
        return line.split(DELIMITER_PATTERN)
            .asSequence()
            .map(::normalizeToken)
            .filter { it.isNotEmpty() }
            .toList()
    }

    private fun looksLikeBundle(nonEmptyLines: List<String>): Boolean {
        return nonEmptyLines.firstOrNull() == BUNDLE_HEADER ||
            nonEmptyLines.any { it.startsWith(PRESET_HEADER_PREFIX) }
    }

    private fun looksLikeCanonicalSingle(lines: List<String>): Boolean {
        val firstNonEmptyIndex = lines.indexOfFirst { normalizeLine(it).isNotEmpty() }
        if (firstNonEmptyIndex == -1) {
            return false
        }

        val separatorOffset = lines
            .drop(firstNonEmptyIndex + 1)
            .indexOfFirst { normalizeLine(it).isEmpty() }
        val separatorIndex = if (separatorOffset == -1) {
            -1
        } else {
            firstNonEmptyIndex + 1 + separatorOffset
        }
        if (separatorIndex == -1) {
            return false
        }

        return lines.drop(separatorIndex + 1).any { normalizeLine(it).isNotEmpty() }
    }

    private fun normalizeLine(raw: String): String {
        return raw
            .replace('\t', ' ')
            .trim(::isTrimWhitespace)
    }

    private fun normalizeToken(raw: String): String {
        return raw
            .trim(::isTrimWhitespace)
            .trimEnd(',', ';')
            .trim(::isTrimWhitespace)
    }

    private fun resolveFallbackName(
        fallbackName: String?,
        defaultName: String
    ): String {
        val resolved = fallbackName
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.trim(::isTrimWhitespace)
            .orEmpty()

        return resolved.ifEmpty { defaultName }
    }

    private fun isTrimWhitespace(char: Char): Boolean {
        return char.isWhitespace() || char == '\u00A0' || char == '\u2007' || char == '\u202F'
    }

    private fun writeBundleTo(
        appendable: Appendable,
        presets: List<ImportedListPreset>
    ) {
        appendable.appendLine(BUNDLE_HEADER)
        appendable.appendLine()
        presets.forEachIndexed { index, preset ->
            appendable.append(PRESET_HEADER_PREFIX)
            appendable.appendLine(preset.name)
            preset.items.forEach { item ->
                appendable.appendLine(item)
            }
            if (index != presets.lastIndex) {
                appendable.appendLine()
            }
        }
    }

    private companion object {
        const val BUNDLE_HEADER: String = "# Randify preset bundle v1"
        const val PRESET_HEADER_PREFIX: String = "[preset] "
        val DELIMITER_PATTERN = "[,;]".toRegex()
    }
}
