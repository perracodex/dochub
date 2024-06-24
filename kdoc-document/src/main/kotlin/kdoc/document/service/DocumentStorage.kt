/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service

import io.ktor.http.content.*
import io.micrometer.core.instrument.Counter
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.SessionContext
import kdoc.base.env.Tracer
import kdoc.base.persistence.pagination.Page
import kdoc.base.persistence.serializers.SUUID
import kdoc.base.persistence.utils.toUUIDOrNull
import kdoc.base.plugins.appMicrometerRegistry
import kdoc.base.security.utils.EncryptionUtils
import kdoc.base.security.utils.SecureIO
import kdoc.base.security.utils.SecureUrl
import kdoc.base.settings.AppSettings
import kdoc.base.utils.NetworkUtils
import kdoc.document.entity.DocumentEntity
import kdoc.document.entity.DocumentFileEntity
import kdoc.document.entity.DocumentRequest
import kdoc.document.errors.DocumentError
import kdoc.document.repository.IDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Document storage service, where all the document storage business logic should be defined.
 */
internal class DocumentStorage(
    @Suppress("unused") private val sessionContext: SessionContext,
    private val documentRepository: IDocumentRepository
) {
    private val tracer = Tracer<DocumentStorage>()

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

        val files: List<MultipartFiles.Response> = MultipartFiles(
            uploadsRoot = uploadRoot,
            cipher = cipher
        ).receive(ownerId = ownerId, groupId = groupId, type = type, multipart = multipart)

        if (files.isEmpty()) {
            tracer.error("No files uploaded.")
            DocumentError.NoDocumentProvided(ownerId = ownerId).raise()
        }

        // Create the document references in the database.

        try {
            val output: MutableList<DocumentEntity> = mutableListOf()
            val targetGroupId: UUID = groupId ?: UUID.randomUUID()

            files.forEach { fileEntry ->
                val location: String = fileEntry.file.path

                val documentRequest = DocumentRequest(
                    ownerId = ownerId,
                    groupId = targetGroupId,
                    name = fileEntry.originalFilename,
                    type = type,
                    description = fileEntry.description,
                    location = location,
                    isCiphered = fileEntry.isCiphered
                )

                val documentId: UUID = documentRepository.create(documentRequest = documentRequest)
                val createdDocument: DocumentEntity = documentRepository.findById(documentId = documentId)!!
                output.add(createdDocument)
            }

            return output
        } catch (e: Exception) {
            tracer.error("Error uploading document: ${e.message}")
            files.forEach { it.file.delete() }
            return emptyList()
        }
    }

    /**
     * Retrieves a {DocumentFileEntity}, given a token and signature.
     *
     * @param token The token to verify.
     * @param signature The signature to verify.
     * @return The [DocumentFileEntity] if the verification is successful, null otherwise.
     */
    suspend fun getSignedDocumentFile(token: String, signature: String): DocumentFileEntity? {
        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"

        val documentId: SUUID? = SecureUrl.verify(
            basePath = basePath,
            token = token,
            signature = signature
        ).toUUIDOrNull()

        if (documentId == null) {
            tracer.warning("Invalid or expired token: $token")
            return null
        }

        return getDocumentFile(documentId = documentId)
    }

    /**
     * Retrieves a reference to the document storage file.
     *
     * @param documentId The ID of the document to be retrieved.
     * @return The [DocumentFileEntity] reference if found, null if not found or the referenced file does not exist.
     */
    private suspend fun getDocumentFile(documentId: UUID): DocumentFileEntity? = withContext(Dispatchers.IO) {
        val documentFile: DocumentFileEntity? = documentRepository.getStorageFile(documentId = documentId)

        documentFile?.let {
            if (documentFile.file.exists()) {
                return@withContext documentFile
            }
        }

        tracer.warning("Document file not found for document ID: $documentId")
        return@withContext null
    }

    /**
     * Processes all documents in the storage to either cipher or decipher based on the provided flag.
     * Documents that are already in the desired cipher state will be ignored.
     *
     * @param cipher If true, ciphers the documents; if false, de-ciphers the documents.
     * @return The number of documents affected by the operation.
     */
    suspend fun changeCipherState(cipher: Boolean): Int = withContext(Dispatchers.IO) {
        val document: Page<DocumentEntity> = documentRepository.findAll()
        if (document.totalElements == 0) {
            tracer.debug("No documents found to change cipher state.")
            return@withContext 0
        }

        var count = 0

        document.content.forEach { documentEntity ->
            if (documentEntity.isCiphered != cipher) {
                if (changeDocumentCipherState(documentEntity, cipher)) {
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
    )
            : Boolean = withContext(Dispatchers.IO) {
        val documentId: UUID = document.id

        val documentFile: DocumentFileEntity? = getDocumentFile(documentId = documentId)
        if (documentFile == null) {
            tracer.warning("Document file not found for document ID: $documentId")
            return@withContext false
        }

        if (documentFile.isCiphered == cipher) {
            tracer.debug("Document ID: $documentId is already in the desired cipher state.")
            return@withContext false
        }

        val tempFilePrefix: String = if (cipher) PREFIX_TEMP_CIPHER else PREFIX_TEMP_DECIPHER
        val tempFile: File = File.createTempFile(tempFilePrefix, null)

        try {
            // Process the cipher state change on a temporary file.
            tracer.debug("Processing document ID: $documentId. Cipher: $cipher. File: ${documentFile.file}")
            documentFile.file.inputStream().buffered().use { inputStream ->
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
            tempFile.copyTo(target = documentFile.file, overwrite = true)

            // Renamed the filename according to the new cipher state.
            val key: String = AppSettings.storage.cipherKey
            val filename: String = documentFile.file.name
            val path: String = documentFile.file.parent

            val newFilename: String = if (cipher) {
                EncryptionUtils.aesEncrypt(data = filename, key = key)
            } else {
                EncryptionUtils.aesDecrypt(data = filename, key = key)
            }

            val newLocation = "$path${File.separator}$newFilename"
            documentFile.file.renameTo(File(newLocation))

            // Update the document record to reflect the change.
            tracer.debug("Changing database cipher state for document ID: $documentId to '$cipher'.")
            documentRepository.setCipherState(documentId = documentId, isCiphered = cipher, location = newLocation)
        } finally {
            tempFile.delete()
        }

        return@withContext true
    }

    /**
     * Backup all files into a ZIP archive.
     * The files are streamed into the ZIP to avoid loading all files into memory.
     *
     * @param outputStream The output stream where the ZIP archive will be written.
     */
    suspend fun backup(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val documents: Page<DocumentEntity> = documentRepository.findAll()

        if (documents.totalElements == 0) {
            tracer.debug("No documents found for backup.")
            return@withContext
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
            documents.content.forEach { document ->
                val documentFile: DocumentFileEntity? = documentRepository.getStorageFile(documentId = document.id)
                val file: File? = documentFile?.file

                if (file != null && file.exists()) {
                    try {
                        FileInputStream(file).use { inputStream ->
                            BufferedInputStream(inputStream).use { bis ->
                                val zipEntry = ZipEntry(file.name)
                                zipStream.putNextEntry(zipEntry)

                                val buffer = ByteArray(size = 4098)
                                var length: Int
                                while (bis.read(buffer).also { length = it } >= 0) {
                                    zipStream.write(buffer, 0, length)
                                }

                                zipStream.closeEntry()
                            }
                        }
                    } catch (e: IOException) {
                        tracer.error("Error adding to backup document file with ID: ${document.id}: ${e.message}")
                    }
                } else {
                    tracer.warning("Document file not found: ${document.id}")
                }
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
    }
}
