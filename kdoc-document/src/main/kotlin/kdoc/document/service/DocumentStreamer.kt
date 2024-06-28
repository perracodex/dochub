/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service

import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import kdoc.base.env.Tracer
import kdoc.base.plugins.appMicrometerRegistry
import kdoc.base.security.utils.SecureIO
import kdoc.base.utils.DateTimeUtils
import kdoc.base.utils.KLocalDateTime
import kdoc.document.entity.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Provides functionality for streaming documents and ZIP archives to clients.
 */
object DocumentStreamer {
    private val tracer = Tracer<DocumentStreamer>()

    /** Metric for tracking the total number of document downloads. */
    val downloadCountMetric: Counter = Counter.builder("kdoc_document_downloads_total")
        .description("Total number of downloaded files")
        .register(appMicrometerRegistry)

    /** Metric for tracking the total number of backups. */
    val backupCountMetric: Counter = Counter.builder("kdoc_document_backups_total")
        .description("Total number of backups")
        .register(appMicrometerRegistry)

    /**
     * Streams a document file to a client.
     *
     * @param document The document to be streamed.
     * @param decipher If true, the document will be deciphered before being streamed.
     * @param respondOutputStream Lambda function to stream the output.
     */
    suspend fun streamDocumentFile(
        document: DocumentEntity,
        decipher: Boolean,
        respondOutputStream: suspend (
            contentDisposition: ContentDisposition,
            contentType: ContentType,
            stream: suspend (OutputStream) -> Unit
        ) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        // Create the response headers.
        val contentDisposition: ContentDisposition = ContentDisposition.Attachment.withParameter(
            key = ContentDisposition.Parameters.FileName,
            value = document.detail.originalName
        )

        // Stream the document file to the output stream.
        respondOutputStream(contentDisposition, ContentType.Application.OctetStream) { outputStream ->
            val documentFile = File(document.detail.path)

            FileInputStream(documentFile).use { inputStream ->
                if (decipher && document.detail.isCiphered) {
                    SecureIO.decipher(input = inputStream, output = outputStream)
                } else {
                    inputStream.copyTo(out = outputStream)
                }
            }
        }
    }

    /**
     * Streams a ZIP archive containing the provided documents.
     *
     * @param filename The name of the ZIP archive.
     * @param documents The documents to be packed into the ZIP archive.
     * @param decipher If true, the documents will be deciphered before being packed.
     * @param respondOutputStream Lambda function to stream the output.
     */
    suspend fun streamZip(
        filename: String,
        documents: List<DocumentEntity>,
        decipher: Boolean,
        respondOutputStream: suspend (
            contentDisposition: ContentDisposition,
            contentType: ContentType,
            stream: suspend (OutputStream) -> Unit
        ) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        // Create the filename for the ZIP archive.
        val currentDate: KLocalDateTime = DateTimeUtils.currentUTCDateTime()
        val formattedDate: String = DateTimeUtils.format(date = currentDate, pattern = DateTimeUtils.Format.YYYY_MM_DD_T_HH_MM_SS)
        val outputFilename = "$filename ($formattedDate).zip"

        // Create the response headers.
        val contentDisposition: ContentDisposition = ContentDisposition.Attachment.withParameter(
            key = ContentDisposition.Parameters.FileName,
            value = outputFilename
        )

        // Create a PipedOutputStream and PipedInputStream to stream the ZIP archive.
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = PipedInputStream(pipedOutputStream)

        // Launch a coroutine to handle the backup and streaming.
        launch {
            pipedOutputStream.use { outputStream ->
                pack(
                    outputStream = outputStream,
                    documents = documents,
                    decipher = decipher
                )
            }
        }

        // Stream the content from pipedInputStream to the response outputStream.
        respondOutputStream(contentDisposition, ContentType.Application.OctetStream) { outputStream ->
            pipedInputStream.use { inputStream ->
                inputStream.copyTo(out = outputStream)
            }
        }
    }

    /**
     * Packs the given documents into a ZIP archive.
     *
     * @param outputStream The output stream where the ZIP archive will be written.
     * @param documents The documents to be packed.
     * @param decipher If true, the documents will be deciphered before being packed.
     */
    private suspend fun pack(
        outputStream: OutputStream,
        documents: List<DocumentEntity>,
        decipher: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) {
            tracer.debug("No documents provided.")
            return@withContext
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
            val fileNameCounts: MutableMap<String, Int> = mutableMapOf()

            documents.forEach { document ->
                val documentFile = File(document.detail.path)

                if (documentFile.exists()) {
                    try {
                        FileInputStream(documentFile).use { inputStream ->
                            BufferedInputStream(inputStream).use { bufferedInputString ->
                                var entryName: String = if (decipher) {
                                    document.detail.originalName
                                } else {
                                    document.detail.storageName
                                }

                                // Ensure unique entry name.
                                entryName = generateUniqueZipEntryName(entryName, fileNameCounts)

                                val zipEntry = ZipEntry(entryName)
                                zipStream.putNextEntry(zipEntry)

                                if (decipher && document.detail.isCiphered) {
                                    SecureIO.decipher(input = bufferedInputString, output = zipStream)
                                } else {
                                    inputStream.copyTo(out = zipStream)
                                }

                                zipStream.closeEntry()
                            }
                        }
                    } catch (e: IOException) {
                        tracer.error("Error adding to zip document with ID: ${document.id}: ${e.message}")
                    }
                } else {
                    tracer.warning("Document file not found: ${document.id}")
                }
            }
        }
    }

    /**
     * Generates a unique entry name for a ZIP archive, by appending a count
     * to the original entry name whenever a duplicate is found.
     * So, for example, if the original entry name is "file.txt" and a duplicate
     * is found, the new entry name will be changed to "file(1).txt".
     *
     * @param entryName The original entry name.
     * @param fileNameCounts A map of entry names and their counts.
     * @return The unique entry name.
     */
    private fun generateUniqueZipEntryName(
        entryName: String,
        fileNameCounts: MutableMap<String, Int>
    ): String {
        val count: Int = fileNameCounts.getOrDefault(entryName, defaultValue = 0)
        fileNameCounts[entryName] = count + 1

        return if (count == 0) {
            entryName
        } else {
            val extensionIndex: Int = entryName.lastIndexOf(char = '.')

            if (extensionIndex == -1) {
                // No extension found.
                "$entryName($count)"
            } else {
                // Extension found.
                val namePart: String = entryName.substring(0, extensionIndex)
                val extensionPart: String = entryName.substring(extensionIndex)
                "$namePart($count)$extensionPart"
            }
        }
    }
}