/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service.managers.upload

import io.ktor.http.content.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.Tracer
import kdoc.base.plugins.appMicrometerRegistry
import kdoc.base.security.snowflake.SnowflakeFactory
import kdoc.base.security.utils.EncryptionUtils
import kdoc.base.security.utils.SecureIO
import kdoc.base.settings.AppSettings
import kdoc.base.utils.DateTimeUtils
import kdoc.base.utils.KLocalDate
import kdoc.document.errors.DocumentError
import kdoc.document.service.DocumentService.Companion.PATH_SEPARATOR
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Handles multipart form data for file uploads with an option to cipher files,
 * ensuring that all received files are correctly stored at the specified location
 * or handled according to their encryption requirement.
 *
 * @property uploadsRoot The root directory path where uploaded files must be saved.
 * @property cipher Flag to determine if uploaded files should be ciphered.
 */
internal class MultipartFileHandler(
    private val uploadsRoot: String,
    private val cipher: Boolean
) {
    private val tracer = Tracer<MultipartFileHandler>()

    /**
     * Data class to encapsulate the details of a persisted file.
     *
     * @property originalFilename The original name of the document file.
     * @property storageFilename The name of the document file as stored on disk.
     * @property location The path location of the document file.
     * @property documentFile The [File] object representing the uploaded file in the storage.
     * @property description Optional description of the document file.
     * @property isCiphered Whether the document file is encrypted.
     */
    data class Response(
        val originalFilename: String,
        val storageFilename: String,
        val location: String,
        val documentFile: File,
        val description: String?,
        val isCiphered: Boolean
    )

    /**
     * Handles the reception of files from a client's request, persisting them to the storage.
     * If any file persistence fails, all saved in this operation are deleted.
     *
     * @param ownerId The ID of the owner of the file.
     * @param groupId Optional ID of the group the file belongs to.
     * @param type The [DocumentType] of the file.
     * @param multipart The MultiPartData from a client's request.
     * @return A list of [Response] objects containing the file details.
     */
    suspend fun receive(
        ownerId: UUID,
        groupId: UUID?,
        type: DocumentType,
        multipart: MultiPartData
    ): List<Response> = withContext(Dispatchers.IO) {
        var responses: List<Response>
        val duration: Long = measureTimeMillis {
            responses = process(multipart = multipart, ownerId = ownerId, groupId = groupId, type = type)
        }
        uploadDurationMetric.record(duration, TimeUnit.MILLISECONDS)
        return@withContext responses
    }

    private suspend fun process(
        ownerId: UUID,
        groupId: UUID?,
        type: DocumentType,
        multipart: MultiPartData
    ): List<Response> = withContext(Dispatchers.IO) {
        val deferredResponses = mutableListOf<Deferred<Response>>()
        val savedFiles = mutableListOf<File>()

        try {
            multipart.forEachPart { part: PartData ->
                if (part is PartData.FileItem) {
                    val filename: String = part.originalFileName
                        ?: throw IllegalArgumentException("No filename provided.")

                    // Asynchronously persist the file.
                    val deferred: Deferred<Response> = async {
                        try {
                            persistFile(
                                ownerId = ownerId,
                                groupId = groupId,
                                type = type,
                                filename = filename,
                                description = part.name,
                                cipherName = cipher,
                                streamProvider = part.streamProvider
                            ).also { response ->
                                savedFiles.add(response.documentFile)
                                uploadsCountMetric.increment()
                            }
                        } catch (e: Exception) {
                            // Log the error and rethrow it to be handled in the outer try-catch.
                            tracer.error("Error persisting file: ${e.message}")
                            throw e
                        } finally {
                            // Dispose of the part to free up resources.
                            part.dispose()
                        }
                    }

                    deferredResponses.add(deferred)
                } else {
                    tracer.error("Unknown part type: $part")
                    part.dispose()
                }
            }

            // Await all the deferred operations to complete and collect responses.
            return@withContext deferredResponses.awaitAll()

        } catch (e: Exception) {
            tracer.error("Error uploading document: $e")
            // If any file persistence fails, delete all saved files.
            savedFiles.forEach { it.delete() }
            throw DocumentError.FailedToPersistUpload(ownerId = ownerId, cause = e)
        }
    }

    /**
     * Persists the file to disk and returns a [Response] object.
     *
     * @param ownerId The ID of the owner of the file.
     * @param groupId Optional ID of the group the file belongs to.
     * @param type The [DocumentType] of the file.
     * @param filename The name of the file.
     * @param description Optional description of the file.
     * @param cipherName Whether the filename should be encrypted.
     * @param streamProvider The provider for the input stream.
     * @return A [Response] object containing the file details.
     */
    private fun persistFile(
        ownerId: UUID,
        groupId: UUID?,
        type: DocumentType,
        filename: String,
        description: String?,
        cipherName: Boolean,
        streamProvider: () -> InputStream
    ): Response {
        val currentDate: KLocalDate = DateTimeUtils.currentUTCDate()
        val datePath: String = "${currentDate.year}$PATH_SEPARATOR" +
                "${currentDate.monthNumber}$PATH_SEPARATOR" +
                "${currentDate.dayOfMonth}"

        val storageFilename: String = buildFilename(
            ownerId = ownerId,
            groupId = groupId,
            type = type,
            originalFileName = filename,
            cipherName = cipherName
        )

        val location = File("$uploadsRoot$PATH_SEPARATOR$datePath")
        location.mkdirs()  // Create directories if they don't exist.

        val documentFile = File(location, storageFilename)
        streamProvider().use { inputStream ->
            documentFile.outputStream().buffered().use { outputStream ->
                if (cipher) {
                    SecureIO.cipher(input = inputStream, output = outputStream)
                } else {
                    inputStream.copyTo(out = outputStream)
                }
            }
        }

        return Response(
            originalFilename = filename,
            storageFilename = storageFilename,
            location = location.path,
            documentFile = documentFile,
            description = description,
            isCiphered = cipher
        )
    }

    /**
     * Builds the path + filename for a document.
     */
    private fun buildFilename(
        ownerId: UUID,
        groupId: UUID?,
        type: DocumentType,
        originalFileName: String,
        cipherName: Boolean
    ): String {
        // A snowflake ID is used to ensure unique filenames and
        // easily identify the producer of the file in a distributed system.
        val snowflakeId: String = SnowflakeFactory.nextId()
        val newFilename: String = "$snowflakeId$NAME_TOKEN_DELIMITER" +
                "$ownerId$NAME_TOKEN_DELIMITER" +
                "$type$NAME_TOKEN_DELIMITER" +
                "${groupId ?: ""}$NAME_TOKEN_DELIMITER" +
                originalFileName

        if (!cipherName) {
            return newFilename
        }

        val key: String = AppSettings.storage.cipherKey
        val encryptedFilename: String = EncryptionUtils.aesEncrypt(data = newFilename, key = key)

        if (EncryptionUtils.aesDecrypt(data = encryptedFilename, key = key) != newFilename) {
            throw IllegalArgumentException("Inconsistent document filename encryption/decryption.")
        }

        return encryptedFilename
    }

    companion object {
        private val uploadsCountMetric: Counter = Counter.builder("kdoc_document_uploads_total")
            .description("Total number of uploaded files")
            .register(appMicrometerRegistry)

        private val uploadDurationMetric: Timer = Timer.builder("kdoc_document_uploads_duration")
            .description("Duration of document upload execution")
            .register(appMicrometerRegistry)

        // Must use a character that is not allowed in filenames by the OS.
        // This is only relevant for non-ciphered filenames.
        private const val NAME_TOKEN_DELIMITER: String = "~"
    }
}
