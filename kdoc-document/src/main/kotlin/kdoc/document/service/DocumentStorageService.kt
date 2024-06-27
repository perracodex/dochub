/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.micrometer.core.instrument.Counter
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.SessionContext
import kdoc.base.env.Tracer
import kdoc.base.persistence.pagination.Page
import kdoc.base.plugins.appMicrometerRegistry
import kdoc.base.security.utils.EncryptionUtils
import kdoc.base.security.utils.SecureIO
import kdoc.base.settings.AppSettings
import kdoc.base.utils.DateTimeUtils
import kdoc.base.utils.KLocalDateTime
import kdoc.document.entity.DocumentEntity
import kdoc.document.entity.DocumentRequest
import kdoc.document.errors.DocumentError
import kdoc.document.repository.IDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Document storage service, where all the document storage business logic should be defined.
 */
internal class DocumentStorageService(
    @Suppress("unused") private val sessionContext: SessionContext,
    private val documentRepository: IDocumentRepository
) {
    private val tracer = Tracer<DocumentStorageService>()

    /**
     * Handles the creation of documents from multipart data.
     * If a group ID is provided, the uploaded files are associated with that group,
     * otherwise a new group ID is generated and associated with the files.
     *
     * If any file persistence fails, all saved in this operation are deleted.
     *
     * @param ownerId The ID of the owner of the document.
     * @param groupId Optional group ID to associate with the uploaded files.
     * @param type The [DocumentType] being uploaded.
     * @param uploadRoot The root path where uploaded files are stored.
     * @param cipher Whether the document should be ciphered.
     * @param multipart The multipart data containing the files and request.
     * @return A list of created DocumentEntity objects or null if the request is invalid.
     */
    suspend fun upload(
        ownerId: UUID,
        groupId: UUID? = null,
        type: DocumentType,
        uploadRoot: String,
        cipher: Boolean,
        multipart: MultiPartData
    ): List<DocumentEntity> {

        // Receive the uploaded files.

        val persistedFiles: List<MultipartFileManager.Response> = MultipartFileManager(
            uploadsRoot = uploadRoot,
            cipher = cipher
        ).receive(ownerId = ownerId, groupId = groupId, type = type, multipart = multipart)

        if (persistedFiles.isEmpty()) {
            tracer.error("No files provided for upload.")
            throw DocumentError.NoDocumentProvided(ownerId = ownerId)
        }

        // Create the document references in the database.

        try {
            val output: MutableList<DocumentEntity> = mutableListOf()
            val targetGroupId: UUID = groupId ?: UUID.randomUUID()

            persistedFiles.forEach { fileEntry ->
                val documentRequest = DocumentRequest(
                    ownerId = ownerId,
                    groupId = targetGroupId,
                    type = type,
                    description = fileEntry.description,
                    originalName = fileEntry.originalFilename,
                    storageName = fileEntry.storageFilename,
                    location = fileEntry.location,
                    isCiphered = fileEntry.isCiphered
                )

                val documentId: UUID = documentRepository.create(documentRequest = documentRequest)
                val createdDocument: DocumentEntity = documentRepository.findById(documentId = documentId)!!
                output.add(createdDocument)
            }

            return output
        } catch (e: Exception) {
            tracer.error("Error uploading document: $e")
            // If any file persistence fails, delete all saved files.
            persistedFiles.forEach { it.documentFile.delete() }
            throw DocumentError.FailedToPersistUpload(ownerId = ownerId)
        }
    }


    /**
     * Processes all documents in the storage to either cipher or decipher based on the provided flag.
     * Documents that are already in the desired cipher state will be ignored.
     *
     * @param cipher If true, ciphers the documents; if false, de-ciphers the documents.
     * @return The number of documents affected by the operation.
     */
    suspend fun changeCipherState(cipher: Boolean): Int = withContext(Dispatchers.IO) {
        val documents: Page<DocumentEntity> = documentRepository.findAll()
        if (documents.totalElements == 0) {
            tracer.debug("No documents found to change cipher state.")
            return@withContext 0
        }

        var count = 0

        documents.content.forEach { document ->
            if (document.detail.isCiphered != cipher) {
                if (changeDocumentCipherState(document = document, cipher = cipher)) {
                    count++
                }
            }
        }

        tracer.debug("Changed cipher state of $count documents to '$cipher'.")
        return@withContext count
    }

    /**
     * Changes the cipher state of a single document.
     *
     * @param document The document to change the cipher state of.
     * @param cipher If true, ciphers the document; if false, de-ciphers the document.
     * @return True if the cipher state was changed, false if the document file was not found.
     */
    private suspend fun changeDocumentCipherState(
        document: DocumentEntity,
        cipher: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val documentId: UUID = document.id

        if (document.detail.isCiphered == cipher) {
            tracer.debug("Document ID: $documentId is already in the desired cipher state.")
            return@withContext false
        }

        val tempFilePrefix: String = if (cipher) PREFIX_TEMP_CIPHER else PREFIX_TEMP_DECIPHER
        val tempFile: File = File.createTempFile(tempFilePrefix, null)

        try {
            // Process the cipher state change on a temporary file.
            tracer.debug("Processing document ID: $documentId. Cipher: $cipher.")
            val documentFile = File(document.detail.path)
            if (!documentFile.exists()) {
                tracer.warning("Document file not found for document ID: $documentId.")
                return@withContext false
            }

            documentFile.inputStream().buffered().use { inputStream ->
                tempFile.outputStream().buffered().use { outputStream ->
                    if (cipher) {
                        SecureIO.cipher(input = inputStream, output = outputStream)
                    } else {
                        SecureIO.decipher(input = inputStream, output = outputStream)
                    }
                }
            }

            // Replace the original file with the new one.
            tracer.debug("Replacing original document file for document ID: $documentId.")
            tempFile.copyTo(target = documentFile, overwrite = true)

            // Renamed the filename according to the new cipher state.
            val key: String = AppSettings.storage.cipherKey
            val filename: String = documentFile.name

            val storageName: String = if (cipher) {
                EncryptionUtils.aesEncrypt(data = filename, key = key)
            } else {
                EncryptionUtils.aesDecrypt(data = filename, key = key)
            }

            val newLocation = "${document.detail.location}$PATH_SEPARATOR$storageName"
            documentFile.renameTo(File(newLocation))

            // Update the document record to reflect the change.
            tracer.debug("Changing database cipher state for document ID: $documentId to '$cipher'.")
            documentRepository.setCipherState(documentId = documentId, isCiphered = cipher, storageName = storageName)
        } finally {
            tempFile.delete()
        }

        return@withContext true
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
            tracer.debug("No documents found provided.")
            return@withContext
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
            documents.forEach { document ->
                val documentFile = File(document.detail.path)
                if (documentFile.exists()) {
                    try {
                        FileInputStream(documentFile).use { inputStream ->
                            BufferedInputStream(inputStream).use { bufferedInputString ->
                                val entryName = if (decipher) {
                                    document.detail.originalName
                                } else {
                                    document.detail.storageName
                                }
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
     * Streams a document file to a client.
     *
     * @param call The application call to respond to.
     * @param document The document to be streamed.
     * @param decipher If true, the document will be deciphered before being streamed.
     */
    suspend fun streamDocumentFile(
        call: ApplicationCall,
        document: DocumentEntity,
        decipher: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                document.detail.originalName
            ).toString()
        )

        call.respondOutputStream(contentType = ContentType.Application.OctetStream) {
            val documentFile = File(document.detail.path)

            FileInputStream(documentFile).use { inputStream ->
                if (decipher && document.detail.isCiphered) {
                    SecureIO.decipher(input = inputStream, output = this)
                } else {
                    inputStream.copyTo(out = this)
                }
            }
        }
    }

    /**
     * Streams a ZIP archive containing the provided documents.
     *
     * @param call The application call to respond to.
     * @param filename The name of the ZIP archive.
     * @param documents The documents to be packed into the ZIP archive.
     * @param decipher If true, the documents will be deciphered before being packed.
     */
    suspend fun streamZip(
        call: ApplicationCall,
        filename: String,
        documents: List<DocumentEntity>,
        decipher: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        val currentDate: KLocalDateTime = DateTimeUtils.currentUTCDateTime()
        val formattedDate: String = DateTimeUtils.format(date = currentDate, pattern = DateTimeUtils.Format.YYYY_MM_DD_T_HH_MM_SS)
        val outputFilename = "$filename ($formattedDate).zip"
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                outputFilename
            ).toString()
        )

        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = PipedInputStream(pipedOutputStream)

        // Launch a coroutine to handle the backup and streaming.
        launch(Dispatchers.IO) {
            pipedOutputStream.use { outputStream ->
                pack(outputStream = outputStream, documents = documents, decipher = decipher)
            }
        }

        call.respondOutputStream(contentType = ContentType.Application.OctetStream) {
            // Stream the content from pipedInputStream to the response outputStream.
            pipedInputStream.use { inputStream ->
                inputStream.copyTo(out = this)
            }
        }
    }

    companion object {
        /** Prefix for temporary cipher files. Used when changing the cipher state of documents. */
        private const val PREFIX_TEMP_CIPHER = "temp-cipher-"

        /** Prefix for temporary decipher files. Used when changing the cipher state of documents. */
        private const val PREFIX_TEMP_DECIPHER = "temp-decipher-"

        /** Metric for tracking the total number of document downloads. */
        val downloadCountMetric: Counter = Counter.builder("kdoc_document_downloads_total")
            .description("Total number of downloaded files")
            .register(appMicrometerRegistry)

        /** Metric for tracking the total number of backups. */
        val backupCountMetric: Counter = Counter.builder("kdoc_document_backups_total")
            .description("Total number of backups")
            .register(appMicrometerRegistry)

        /** Token delimiter used in document filenames. */
        val PATH_SEPARATOR: String = File.separator
    }
}
