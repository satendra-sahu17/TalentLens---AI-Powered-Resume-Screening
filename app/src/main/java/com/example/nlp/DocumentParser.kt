package com.example.nlp

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.example.data.model.ResumeFileType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class ParsedFileResult(
    val fileName: String,
    val fileType: ResumeFileType,
    val fileSizeBytes: Long,
    val extractedText: String
)

object DocumentParser {
    private const val MAX_FILE_SIZE_BYTES = 15L * 1024L * 1024L
    @Volatile
    private var pdfBoxInitialized = false

    private fun ensurePdfBox(context: Context) {
        if (!pdfBoxInitialized) {
            synchronized(this) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    suspend fun parseUri(context: Context, uri: Uri): Result<ParsedFileResult> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                var fileName = "resume"
                var declaredSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) declaredSize = cursor.getLong(sizeIdx)
                    }
                }

                if (declaredSize > MAX_FILE_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("'$fileName' exceeds the 15 MB limit.")
                    )
                }

                val mimeType = resolver.getType(uri).orEmpty().lowercase()
                val lowerName = fileName.lowercase()
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val buffer = input.readBytes()
                    buffer
                } ?: return@withContext Result.failure(
                    IllegalStateException("Unable to open '$fileName'.")
                )

                if (bytes.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("'$fileName' is empty.")
                    )
                }
                if (bytes.size > MAX_FILE_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("'$fileName' exceeds the 15 MB limit.")
                    )
                }

                val isPdf = lowerName.endsWith(".pdf") || mimeType == "application/pdf"
                val isDocx = lowerName.endsWith(".docx") ||
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

                when {
                    isPdf -> {
                        if (!hasPdfMagicHeader(bytes)) {
                            return@withContext Result.failure(
                                IllegalArgumentException("'$fileName' does not appear to be a valid PDF file.")
                            )
                        }
                        ensurePdfBox(context)
                        val text = extractPdfText(bytes)
                        if (text.isBlank()) {
                            return@withContext Result.failure(
                                IllegalArgumentException("No selectable text found in '$fileName'. Scanned image-only PDFs are not supported.")
                            )
                        }
                        Result.success(
                            ParsedFileResult(
                                fileName = fileName,
                                fileType = ResumeFileType.PDF,
                                fileSizeBytes = bytes.size.toLong(),
                                extractedText = text
                            )
                        )
                    }
                    isDocx -> {
                        if (!hasZipMagicHeader(bytes)) {
                            return@withContext Result.failure(
                                IllegalArgumentException("'$fileName' is not a valid DOCX archive.")
                            )
                        }
                        val text = extractDocxText(bytes)
                        if (text.isBlank()) {
                            return@withContext Result.failure(
                                IllegalArgumentException("No readable text found in '$fileName'.")
                            )
                        }
                        Result.success(
                            ParsedFileResult(
                                fileName = fileName,
                                fileType = ResumeFileType.DOCX,
                                fileSizeBytes = bytes.size.toLong(),
                                extractedText = text
                            )
                        )
                    }
                    else -> Result.failure(
                        IllegalArgumentException("Unsupported format for '$fileName'. Upload PDF or DOCX resumes only.")
                    )
                }
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        "Failed to parse document: ${e.localizedMessage ?: "corrupted file"}",
                        e
                    )
                )
            }
        }

    private fun hasPdfMagicHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val header = String(bytes, 0, minOf(16, bytes.size), Charsets.US_ASCII)
        return header.contains("%PDF")
    }

    private fun hasZipMagicHeader(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()
    }

    private fun extractPdfText(bytes: ByteArray): String {
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            if (document.isEncrypted) {
                throw IllegalArgumentException("Encrypted PDFs cannot be parsed.")
            }
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            return stripper.getText(document).trim()
        }
    }

    private fun extractDocxText(bytes: ByteArray): String {
        val sb = StringBuilder()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlBytes = zis.readBytes()
                    val parser = Xml.newPullParser()
                    parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "t" || parser.name == "w:t") {
                                    sb.append(parser.nextText())
                                } else if (parser.name == "tab" || parser.name == "w:tab") {
                                    sb.append(" ")
                                } else if (parser.name == "br" || parser.name == "w:br") {
                                    sb.append("\n")
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "p" || parser.name == "w:p") {
                                    sb.append("\n")
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return sb.toString()
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
