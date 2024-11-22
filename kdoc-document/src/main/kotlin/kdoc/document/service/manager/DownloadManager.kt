/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service.manager

import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import kdoc.core.env.Telemetry
import kdoc.core.env.Tracer
import kdoc.core.security.util.SecureIO
import kdoc.core.util.DateTimeUtils
import kdoc.core.util.DateTimeUtils.current
import kdoc.core.util.DateTimeUtils.format
import kdoc.document.error.DocumentError
import kdoc.document.model.Document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Provides functionality for streaming documents and ZIP archives to clients.
 */
internal object DownloadManager {
    private val tracer: Tracer = Tracer<DownloadManager>()

    /** Metric for tracking the total number of document downloads. */
    val downloadCountMetric: Counter = Telemetry.registerCounter(
        name = "kdoc_document_downloads_total",
        description = "Total number of downloaded files"
    )

    /** Metric for tracking the total number of backups. */
    val backupCountMetric: Counter = Telemetry.registerCounter(
        name = "kdoc_document_backups_total",
        description = "Total number of backups"
    )

    /**
     * Handler for streaming content to the client.
     *
     * @property contentDisposition Indicates how the content should be handled (e.g., as an attachment with a specific filename).
     * @property contentType The content type of the data being streamed, typically used to indicate the MIME type of the response.
     * @property stream A suspend function that handles the actual streaming of content to the provided output stream.
     */
    data class StreamHandler(
        val contentDisposition: ContentDisposition,
        val contentType: ContentType,
        val stream: suspend (OutputStream) -> Unit
    )

    /**
     * Prepares the [StreamHandler] for documents based on the need to archive or decipher.
     * Determines the content type and disposition to facilitate the download as an attachment.
     *
     * @param documents A list of documents to be streamed.
     * @param decipher Whether to decipher each document before streaming.
     * @param archiveFilename Base filename for the archive if multiple documents need archiving.
     * @param archiveAlways Whether to always archive the documents, even if there is only one.
     * @return A [StreamHandler] configured with the appropriate settings for content type and disposition.
     */
    fun prepareStream(
        documents: List<Document>,
        decipher: Boolean,
        archiveFilename: String,
        archiveAlways: Boolean
    ): StreamHandler {
        // Determine the filename based on the number of documents.
        val filename: String = if (documents.size == 1 && !archiveAlways) {
            documents.first().detail.originalName
        } else {
            newArchiveFilename(suffix = archiveFilename)
        }

        // Define the content type and disposition for the download.
        val contentType: ContentType = ContentType.Application.OctetStream
        val contentDisposition: ContentDisposition = ContentDisposition.Attachment.withParameter(
            key = ContentDisposition.Parameters.FileName,
            value = filename
        )

        // Define the stream handler function, which decides how to stream the content.
        val streamHandler: suspend (OutputStream) -> Unit = { outputStream ->
            if (documents.size == 1 && !archiveAlways) {
                val document: Document = documents.first()
                streamSingleDocument(document = document, decipher = decipher, outputStream = outputStream)
            } else {
                streamArchive(documents = documents, decipher = decipher, outputStream = outputStream)
            }
        }

        return StreamHandler(
            contentDisposition = contentDisposition,
            contentType = contentType,
            stream = streamHandler
        )
    }

    /**
     * Streams an archive containing the provided documents while potentially deciphering them.
     *
     * @param documents The documents to be packed into the archive.
     * @param decipher If true, the documents will be deciphered before being packed.
     * @param outputStream The output stream to which the archive is written.
     */
    private suspend fun streamArchive(
        documents: List<Document>,
        decipher: Boolean,
        outputStream: OutputStream
    ): Unit = withContext(Dispatchers.IO) {
        // Create a PipedOutputStream and PipedInputStream to stream the ZIP archive.
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = PipedInputStream(pipedOutputStream)

        // Deferred to capture errors from the launched coroutine
        val deferredPackaging = CompletableDeferred<Unit>()

        // Launch a coroutine to stream the packaging of the documents into the archive.
        launch {
            runCatching {
                pipedOutputStream.use { pipedStream ->
                    pack(
                        documents = documents,
                        decipher = decipher,
                        outputStream = pipedStream
                    )
                }
            }.onFailure { e ->
                deferredPackaging.completeExceptionally(e)
            }.onSuccess {
                deferredPackaging.complete(Unit)
            }
        }

        // Stream the content from pipedInputStream to the response outputStream.
        val streamingResult: Result<Unit> = runCatching {
            pipedInputStream.use { inputStream ->
                inputStream.copyTo(out = outputStream)
            }
        }

        // Handle any exceptions that occurred during the packing process.
        try {
            deferredPackaging.await()
        } catch (e: Exception) {
            tracer.error("Error during document packaging: $e")
            throw DocumentError.FailedToStreamDownload(ownerId = documents.first().ownerId, cause = e)
        }

        // Handle the result of the streaming operation.
        streamingResult.onFailure { e ->
            tracer.error("Error during document streaming: $e")
            throw DocumentError.FailedToStreamDownload(ownerId = documents.first().ownerId, cause = e)
        }
    }

    /**
     * Packs the given documents into an archive, potentially deciphering them before packing.
     *
     * @param documents The documents to be packed.
     * @param decipher If true, the documents will be deciphered before being packed.
     * @param outputStream The output stream where the ZIP archive will be written.
     */
    private fun pack(
        documents: List<Document>,
        decipher: Boolean,
        outputStream: OutputStream
    ) {
        if (documents.isEmpty()) {
            tracer.debug("No documents provided.")
            return
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
     * Generates a unique entry name for an archive entry, managing duplicate names
     * by appending a count to ensure each entry is unique.
     * For example, if the original entry name is "file.txt" and a duplicate
     * is found, the new entry name will be changed to "file(1).txt".
     *
     * @param entryName The original entry name.
     * @param fileNameCounts A map tracking the number of occurrences of each entry name.
     * @return The modified entry name, ensuring uniqueness within the archive.
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

    /**
     * Creates a new filename for an archive, incorporating the current date and time for uniqueness.
     *
     * @param suffix The suffix of the archive filename, related to the operation or document type.
     * @return A uniquely time-stamped archive filename.
     */
    private fun newArchiveFilename(suffix: String): String {
        val formattedDate: String = LocalDateTime.current().format(pattern = DateTimeUtils.Format.YYYY_MM_DD_T_HH_MM_SS)
        return "$suffix ($formattedDate).zip"
    }

    /**
     * Streams a single document file, deciphering it if necessary.
     *
     * @param document The document to be streamed.
     * @param decipher If true, the document will be deciphered before streaming.
     * @param outputStream The output stream to write the document to.
     */
    private suspend fun streamSingleDocument(
        document: Document,
        decipher: Boolean,
        outputStream: OutputStream
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val documentFile = File(document.detail.path)

                FileInputStream(documentFile).use { inputStream ->
                    if (decipher && document.detail.isCiphered) {
                        SecureIO.decipher(input = inputStream, output = outputStream)
                    } else {
                        inputStream.copyTo(out = outputStream)
                    }
                }
            }.onFailure { e ->
                tracer.error("Error streaming document with ID: ${document.id}: $e")
                throw DocumentError.FailedToStreamDownload(ownerId = document.ownerId, cause = e)
            }
        }
    }
}