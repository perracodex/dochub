/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service.managers.upload

import io.ktor.http.content.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import kdoc.core.database.schema.document.types.DocumentType
import kdoc.core.env.Telemetry
import kdoc.core.env.Tracer
import kdoc.core.security.snowflake.SnowflakeFactory
import kdoc.core.security.utils.EncryptionUtils
import kdoc.core.settings.AppSettings
import kdoc.core.utils.DateTimeUtils
import kdoc.core.utils.KLocalDate
import kdoc.document.errors.DocumentError
import kdoc.document.service.DocumentService.Companion.PATH_SEPARATOR
import kdoc.document.service.managers.upload.annotation.UploadAPI
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.uuid.Uuid

/**
 * Handles multipart form data for file uploads with an option to cipher files,
 * ensuring that all received files are correctly stored at the specified location
 * or handled according to their encryption requirement.
 *
 * @property uploadsRoot The root directory path where uploaded files must be saved.
 * @property cipher Flag to determine if uploaded files should be ciphered.
 */
@UploadAPI
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
     * @property description Optional description of the document file.
     * @property isCiphered Whether the document file is encrypted.
     * @property size The size of the document in bytes. Without encryption.
     * @property file The document reference in the storage.
     */
    data class Response(
        val originalFilename: String,
        val storageFilename: String,
        val location: String,
        val description: String?,
        val isCiphered: Boolean,
        val size: Long,
        private val file: File,
    ) {
        fun delete() {
            file.delete()
        }
    }

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
        ownerId: Uuid,
        groupId: Uuid?,
        type: DocumentType,
        multipart: MultiPartData
    ): List<Response> {
        var responses: List<Response>
        val duration: Long = measureTimeMillis {
            responses = process(multipart = multipart, ownerId = ownerId, groupId = groupId, type = type)
        }
        uploadDurationMetric.record(duration, TimeUnit.MILLISECONDS)
        return responses
    }

    private suspend fun process(
        ownerId: Uuid,
        groupId: Uuid?,
        type: DocumentType,
        multipart: MultiPartData
    ): List<Response> = withContext(Dispatchers.IO) {
        val deferredResponses = mutableListOf<Deferred<Response>>()
        val responses = mutableListOf<Response>()

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
                                responses.add(response)
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

            // Cancel all deferred operations and delete all saved files.
            deferredResponses.onEach { it.cancel() }
                .mapNotNull { deferred -> runCatching { deferred.await() }.getOrNull() }
                .forEach { response -> response.delete() }

            throw DocumentError.FailedToPersistUpload(ownerId = ownerId, cause = e)
        }
    }

    /**
     * Builds the document storage filename and persists it to the storage.
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
        ownerId: Uuid,
        groupId: Uuid?,
        type: DocumentType,
        filename: String,
        description: String?,
        cipherName: Boolean,
        streamProvider: () -> InputStream
    ): Response {
        val currentDate: KLocalDate = DateTimeUtils.currentDate()
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

        val fileDetails: StorageFileIO.FileDetails = streamProvider().use { rawInputStream ->
            StorageFileIO.save(
                uploadsRoot = uploadsRoot,
                path = datePath,
                storageFilename = storageFilename,
                cipher = cipher,
                inputStream = rawInputStream
            )
        }

        return Response(
            originalFilename = filename,
            storageFilename = storageFilename,
            location = fileDetails.location,
            file = fileDetails.file,
            description = description,
            isCiphered = cipher,
            size = fileDetails.fileSize
        )
    }

    /**
     * Builds the path + filename for a document.
     *
     * @param ownerId The ID of the owner of the file.
     * @param groupId Optional ID of the group the file belongs to.
     * @param type The [DocumentType] of the file.
     * @param originalFileName The original name of the file.
     * @param cipherName Whether the filename should be encrypted.
     */
    private fun buildFilename(
        ownerId: Uuid,
        groupId: Uuid?,
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
        /** Metrics for tracking document uploads. */
        private val uploadsCountMetric: Counter = Telemetry.registerCounter(
            name = "kdoc_document_uploads_total",
            description = "Total number of uploaded files"
        )

        /** Timer for tracking the duration of document uploads. */
        private val uploadDurationMetric: Timer = Telemetry.registerTimer(
            name = "kdoc_document_uploads_duration",
            description = "Duration of document upload execution"
        )

        /**
         * Delimiter used to separate tokens in the filename.
         * Must use a character that is allowed in filenames by the OS.
         * This is only relevant for non-ciphered filenames.
         */
        private const val NAME_TOKEN_DELIMITER: String = "~"
    }
}
