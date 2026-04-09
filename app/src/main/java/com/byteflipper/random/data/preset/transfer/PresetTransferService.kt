package com.byteflipper.random.data.preset.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class PresetTransferService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ListPresetRepository
) {

    private val csvCodec = PresetTransferCsvCodec()
    private val jsonCodec = PresetTransferCodec()
    private val textCodec = PresetTransferTextCodec()

    fun exportPreset(preset: ListPreset, format: PresetTransferFormat): PresetTransferPayload {
        val content = when (format) {
            PresetTransferFormat.Csv -> csvCodec.exportPreset(preset)
            PresetTransferFormat.Json -> jsonCodec.exportPreset(preset)
            PresetTransferFormat.Txt -> {
                textCodec.exportPreset(
                    ImportedListPreset(
                        name = preset.name,
                        items = preset.items
                    )
                )
            }
        }

        return PresetTransferPayload(
            format = format,
            fileName = "${slugify(preset.name)}${format.singleFileSuffix}",
            mimeType = format.mimeType,
            content = content,
            isBundle = false
        )
    }

    suspend fun exportPresets(
        presets: List<ListPreset>,
        format: PresetTransferFormat
    ): PresetTransferPayload {
        if (presets.isEmpty()) {
            throw PresetTransferException.NoPresets
        }
        val date = LocalDate.now().toString()
        val content = when (format) {
            PresetTransferFormat.Csv -> csvCodec.exportBundle(presets)
            PresetTransferFormat.Json -> jsonCodec.exportBundle(presets)
            PresetTransferFormat.Txt -> textCodec.exportBundle(
                presets = presets.map { preset ->
                    ImportedListPreset(
                        name = preset.name,
                        items = preset.items
                    )
                }
            )
        }
        return PresetTransferPayload(
            format = format,
            fileName = "random-presets-$date${format.bundleFileSuffix}",
            mimeType = format.mimeType,
            content = content,
            isBundle = true,
            sourcePresetIds = presets.map { it.id }
        )
    }

    suspend fun parseImportFromUri(uri: Uri): ParsedPresetImport = withContext(Dispatchers.IO) {
        val fileName = getDisplayName(uri)
        val input = readText(uri)
        parseImport(
            input = input,
            fallbackName = fileName,
            detectedFormat = detectImportFormat(fileName, input),
            sourceLabel = fileName
        )
    }

    fun parseImportFromClipboard(text: String): ParsedPresetImport {
        return parseImport(
            input = text,
            fallbackName = null,
            detectedFormat = detectImportFormat(fileName = null, input = text),
            sourceLabel = context.getString(R.string.clipboard)
        )
    }

    suspend fun commitImport(
        parsedImport: ParsedPresetImport,
        mode: PresetImportMode
    ): PresetImportResult = withContext(Dispatchers.IO) {
        val importedCount = repository.importPresets(parsedImport.presets, mode).size
        PresetImportResult(
            importedCount = importedCount,
            skippedCount = parsedImport.issues.size,
            issues = parsedImport.issues,
            isBundle = parsedImport.isBundle,
            format = parsedImport.format
        )
    }

    fun importFromCsv(input: String, fallbackName: String?): ParsedPresetImport {
        return csvCodec.importFromCsv(
            input = input,
            fallbackName = fallbackName,
            defaultName = context.getString(R.string.imported_preset)
        )
    }

    fun importFromJson(input: String): ParsedPresetImport {
        return jsonCodec.importFromJson(input)
    }

    fun importFromText(input: String, fallbackName: String?): ParsedPresetImport {
        return textCodec.importFromText(
            input = input,
            fallbackName = fallbackName,
            defaultName = context.getString(R.string.imported_preset)
        )
    }

    suspend fun writeExportToUri(payload: PresetTransferPayload, uri: Uri) = withContext(Dispatchers.IO) {
        val outputStream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Unable to open output stream")
        outputStream.bufferedWriter().use { writer ->
            when {
                payload.isBundle && payload.format == PresetTransferFormat.Txt -> {
                    val presets = repository.getAllByIds(payload.sourcePresetIds).map { preset ->
                        ImportedListPreset(
                            name = preset.name,
                            items = preset.items
                        )
                    }
                    textCodec.writeBundle(writer, presets)
                }
                payload.isBundle && payload.format == PresetTransferFormat.Csv -> {
                    csvCodec.writeBundle(writer, repository.getAllByIds(payload.sourcePresetIds))
                }
                else -> {
                    writer.write(payload.content)
                }
            }
        }
    }

    suspend fun createShareIntent(
        payload: PresetTransferPayload,
        subject: String
    ): Intent = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "shared_presets").apply { mkdirs() }
        val file = File(directory, payload.fileName)
        if (payload.isBundle && payload.format == PresetTransferFormat.Txt) {
            file.bufferedWriter().use { writer ->
                textCodec.writeBundle(
                    writer,
                    repository.getAllByIds(payload.sourcePresetIds).map { preset ->
                        ImportedListPreset(
                            name = preset.name,
                            items = preset.items
                        )
                    }
                )
            }
        } else if (payload.isBundle && payload.format == PresetTransferFormat.Csv) {
            file.bufferedWriter().use { writer ->
                csvCodec.writeBundle(writer, repository.getAllByIds(payload.sourcePresetIds))
            }
        } else {
            file.writeText(payload.content)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(
                context.contentResolver,
                payload.fileName,
                uri
            )
        }

        Intent.createChooser(shareIntent, subject).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun replaceMatchingImport(uri: Uri): PresetImportResult = withContext(Dispatchers.IO) {
        commitImport(parseImportFromUri(uri), PresetImportMode.ReplaceMatching)
    }

    private fun readText(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream")
        inputStream.use { stream ->
            val buffer = ByteArray(8 * 1024)
            val output = ByteArrayOutputStream()
            var totalBytes = 0

            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break

                totalBytes += read
                if (totalBytes > MAX_IMPORT_BYTES) {
                    throw PresetTransferException.FileTooLarge
                }
                output.write(buffer, 0, read)
            }

            return decodeText(output.toByteArray())
        }
    }

    private fun detectImportFormat(fileName: String?, input: String): PresetTransferFormat {
        val byName = PresetTransferFormat.fromFileName(fileName)
        if (byName != null) {
            return byName
        }

        val trimmed = input.trimStart()
        return when {
            trimmed.startsWith("{") -> PresetTransferFormat.Json
            looksLikeCsv(trimmed) -> PresetTransferFormat.Csv
            trimmed.isNotEmpty() -> PresetTransferFormat.Txt
            else -> throw PresetTransferException.UnsupportedFileFormat(fileName)
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        return uri.lastPathSegment
    }

    private fun decodeText(bytes: ByteArray): String {
        if (bytes.isEmpty()) {
            return ""
        }

        if (bytes.startsWithUtf8Bom()) {
            return decodeStrict(bytes.copyOfRange(3, bytes.size), StandardCharsets.UTF_8)
        }
        if (bytes.startsWithUtf16LeBom()) {
            return decodeStrict(bytes.copyOfRange(2, bytes.size), StandardCharsets.UTF_16LE)
        }
        if (bytes.startsWithUtf16BeBom()) {
            return decodeStrict(bytes.copyOfRange(2, bytes.size), StandardCharsets.UTF_16BE)
        }

        return try {
            decodeStrict(bytes, StandardCharsets.UTF_8)
        } catch (_: CharacterCodingException) {
            tryDecodeWithoutBom(bytes)
        }
    }

    private fun tryDecodeWithoutBom(bytes: ByteArray): String {
        val nullsAtEven = bytes.indices.count { it % 2 == 0 && bytes[it] == 0.toByte() }
        val nullsAtOdd = bytes.indices.count { it % 2 == 1 && bytes[it] == 0.toByte() }
        val likelyUtf16Le = nullsAtOdd > bytes.size / 8
        val likelyUtf16Be = nullsAtEven > bytes.size / 8

        return when {
            likelyUtf16Le -> decodeStrictOrUnsupported(bytes, StandardCharsets.UTF_16LE)
            likelyUtf16Be -> decodeStrictOrUnsupported(bytes, StandardCharsets.UTF_16BE)
            else -> throw PresetTransferException.UnsupportedEncoding
        }
    }

    private fun decodeStrictOrUnsupported(bytes: ByteArray, charset: Charset): String {
        return try {
            decodeStrict(bytes, charset)
        } catch (_: CharacterCodingException) {
            throw PresetTransferException.UnsupportedEncoding
        }
    }

    private fun decodeStrict(bytes: ByteArray, charset: Charset): String {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
    }

    private fun parseImport(
        input: String,
        fallbackName: String?,
        detectedFormat: PresetTransferFormat,
        sourceLabel: String?
    ): ParsedPresetImport {
        val parsed = when (detectedFormat) {
            PresetTransferFormat.Csv -> importFromCsv(input, fallbackName)
            PresetTransferFormat.Json -> importFromJson(input)
            PresetTransferFormat.Txt -> importFromText(input, fallbackName)
        }
        return parsed.copy(sourceLabel = sourceLabel)
    }

    private fun looksLikeCsv(input: String): Boolean {
        val firstLine = input.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        return firstLine == "name,item" || firstLine == "\"name\",\"item\""
    }

    private fun ByteArray.startsWithUtf8Bom(): Boolean {
        return size >= 3 &&
            this[0] == 0xEF.toByte() &&
            this[1] == 0xBB.toByte() &&
            this[2] == 0xBF.toByte()
    }

    private fun ByteArray.startsWithUtf16LeBom(): Boolean {
        return size >= 2 &&
            this[0] == 0xFF.toByte() &&
            this[1] == 0xFE.toByte()
    }

    private fun ByteArray.startsWithUtf16BeBom(): Boolean {
        return size >= 2 &&
            this[0] == 0xFE.toByte() &&
            this[1] == 0xFF.toByte()
    }

    private fun slugify(source: String): String {
        val normalized = Normalizer.normalize(source.trim(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)

        val builder = StringBuilder()
        var lastWasSeparator = false

        normalized.forEach { char ->
            when {
                char.isLetterOrDigit() -> {
                    builder.append(char)
                    lastWasSeparator = false
                }
                char == '-' || char == '_' || char.isWhitespace() -> {
                    if (!lastWasSeparator && builder.isNotEmpty()) {
                        builder.append('-')
                        lastWasSeparator = true
                    }
                }
                else -> {
                    if (!lastWasSeparator && builder.isNotEmpty()) {
                        builder.append('-')
                        lastWasSeparator = true
                    }
                }
            }
        }

        val slug = builder
            .toString()
            .trim('-')

        return slug.ifEmpty { DEFAULT_SINGLE_FILE_NAME }
    }

    private companion object {
        const val DEFAULT_SINGLE_FILE_NAME: String = "preset"
        const val MAX_IMPORT_BYTES: Int = 16 * 1024 * 1024
    }
}
