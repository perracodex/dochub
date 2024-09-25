/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service

import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import kdoc.core.context.SessionContext
import kdoc.core.env.Tracer
import kdoc.core.persistence.utils.toUuidOrNull
import kdoc.core.security.utils.SecureUrl
import kdoc.core.settings.AppSettings
import kdoc.core.utils.NetworkUtils
import kdoc.document.model.Document
import kdoc.document.model.DocumentFilterSet
import kdoc.document.model.DocumentRequest
import kdoc.document.repository.IDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.Uuid

/**
 * Document service, where all the documents business logic should be defined.
 */
internal class DocumentService(
    @Suppress("unused") private val sessionContext: SessionContext,
    private val documentRepository: IDocumentRepository
) {
    private val tracer = Tracer<DocumentService>()

    /**
     * Retrieves a document by its ID.
     *
     * @param documentId The ID of the document to be retrieved.
     * @return The resolved [Document] if found, null otherwise.
     */
    suspend fun findById(documentId: Uuid): Document? = withContext(Dispatchers.IO) {
        return@withContext documentRepository.findById(documentId = documentId)
    }

    /**
     * Retrieves a document by its owner ID.
     *
     * @param ownerId The owner ID of the document to be retrieved.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    suspend fun findByOwnerId(ownerId: Uuid, pageable: Pageable?): Page<Document> = withContext(Dispatchers.IO) {
        return@withContext documentRepository.findByOwnerId(ownerId = ownerId, pageable = pageable)
    }

    /**
     * Retrieves all document entries by group ID.
     *
     * @param groupId The group the documents belongs to.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    suspend fun findByGroupId(groupId: Uuid, pageable: Pageable? = null): Page<Document> = withContext(Dispatchers.IO) {
        return@withContext documentRepository.findByGroupId(groupId = groupId, pageable = pageable)
    }

    /**
     * Retrieves all document entries.
     *
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    suspend fun findAll(pageable: Pageable? = null): Page<Document> = withContext(Dispatchers.IO) {
        return@withContext documentRepository.findAll(pageable = pageable)
    }

    /**
     * Retrieves all document entries matching the provided [filterSet].
     *
     * @param filterSet The [DocumentFilterSet] to be applied.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    suspend fun search(filterSet: DocumentFilterSet, pageable: Pageable? = null): Page<Document> = withContext(Dispatchers.IO) {
        return@withContext documentRepository.search(filterSet = filterSet, pageable = pageable)
    }

    /**
     * Creates a new document.
     *
     * @param request The document to be created.
     * @return The newly created [Document].
     */
    suspend fun create(request: DocumentRequest): Document = withContext(Dispatchers.IO) {
        tracer.debug("Creating a new document.")
        return@withContext documentRepository.create(request = request)
    }

    /**
     * Updates a document's details.
     *
     * @param documentId The ID of the document to be updated.
     * @param request The new details for the document.
     * @return The Updated [Document], or null if not found.
     */
    @Suppress("unused")
    suspend fun update(
        documentId: Uuid,
        request: DocumentRequest
    ): Document? = withContext(Dispatchers.IO) {
        tracer.debug("Updating document with ID: $documentId.")
        return@withContext documentRepository.update(documentId = documentId, request = request)
    }

    /**
     * Deletes a document using the provided ID.
     *
     * @param documentId The ID of the document to be deleted.
     * @return The number of delete records.
     */
    suspend fun delete(documentId: Uuid): Int = withContext(Dispatchers.IO) {
        tracer.debug("Deleting document with ID: $documentId.")
        return@withContext documentRepository.delete(documentId = documentId)
    }

    /**
     * Deletes all documents by group ID.
     *
     * @param groupId The group ID to be used for deletion.
     * @return The number of deleted records.
     */
    suspend fun deleteByGroup(groupId: Uuid): Int = withContext(Dispatchers.IO) {
        tracer.debug("Deleting all documents by group ID: $groupId.")
        return@withContext documentRepository.deleteByGroup(groupId = groupId)
    }

    /**
     * Deletes all documents.
     *
     * @return The number of deleted records.
     */
    suspend fun deleteAll(): Int = withContext(Dispatchers.IO) {
        tracer.debug("Deleting all documents.")
        return@withContext documentRepository.deleteAll()
    }

    /**
     * Retrieves the total count of documents.
     *
     * @return The total count of existing records.
     */
    suspend fun count(): Int = withContext(Dispatchers.IO) {
        return@withContext documentRepository.count()
    }

    /**
     * Retrieves a list of [Document] references based on the provided token and signature.
     * If the signature is invalid or expired, the method will return null.
     *
     * @param token The token to verify.
     * @param signature The signature to verify.
     * @return The [Document] if the verification is successful, null otherwise.
     */
    suspend fun findBySignature(token: String, signature: String): List<Document>? {
        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"
        val decodedToken: String? = SecureUrl.verify(
            basePath = basePath,
            token = token,
            signature = signature
        )

        decodedToken ?: run {
            tracer.warning("Invalid or expired token: $token")
            return null
        }

        val params: Map<String, String> = decodedToken.split("&").associate {
            val (key, value) = it.split("=")
            key.lowercase() to value.trim()
        }

        val documentId: Uuid? = params["document_id"]?.toUuidOrNull()
        val groupId: Uuid? = params["group_id"]?.toUuidOrNull()
        (documentId ?: groupId) ?: run {
            tracer.error("No document ID or group ID provided.")
            throw IllegalArgumentException("No document ID or group ID provided.")
        }

        return withContext(Dispatchers.IO) {
            return@withContext search(
                filterSet = DocumentFilterSet(
                    id = documentId,
                    groupId = groupId
                )
            ).content
        }
    }

    companion object {
        /** The file path's system-dependent name-separator character. */
        val PATH_SEPARATOR: String = File.separator
    }
}
