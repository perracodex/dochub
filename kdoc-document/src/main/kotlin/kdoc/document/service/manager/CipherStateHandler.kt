/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service.manager

import io.perracodex.exposed.pagination.Page
import kdoc.core.context.SessionContext
import kdoc.core.env.Tracer
import kdoc.core.security.util.EncryptionUtils
import kdoc.core.security.util.SecureIO
import kdoc.core.settings.AppSettings
import kdoc.document.model.Document
import kdoc.document.repository.IDocumentRepository
import kdoc.document.service.DocumentService.Companion.PATH_SEPARATOR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.Uuid

/**
 * Handles the ciphering and de-ciphering of document files in the storage.
 */
internal class CipherStateHandler(
    @Suppress("unused") private val sessionContext: SessionContext,
    private val documentRepository: IDocumentRepository
) {
    private val tracer: Tracer = Tracer<CipherStateHandler>()

    /**
     * Processes all documents in the storage to either cipher or decipher based on the provided flag.
     * Documents that are already in the desired cipher state will be ignored.
     *
     * @param cipher If true, ciphers the documents; if false, de-ciphers the documents.
     * @return The number of documents affected by the operation.
     */
    suspend fun changeState(cipher: Boolean): Int = withContext(Dispatchers.IO) {
        val documents: Page<Document> = documentRepository.findAll()
        if (documents.content.isEmpty()) {
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
        document: Document,
        cipher: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val documentId: Uuid = document.id

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

    companion object {
        /** Prefix for temporary cipher files. Used when changing the cipher state of documents. */
        private const val PREFIX_TEMP_CIPHER = "temp-cipher-"

        /** Prefix for temporary decipher files. Used when changing the cipher state of documents. */
        private const val PREFIX_TEMP_DECIPHER = "temp-decipher-"
    }
}
