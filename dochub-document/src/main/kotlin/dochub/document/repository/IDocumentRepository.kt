/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.repository

import dochub.document.model.Document
import dochub.document.model.DocumentFilterSet
import dochub.document.model.DocumentRequest
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import kotlin.uuid.Uuid

/**
 * Responsible for managing documents data.
 */
internal interface IDocumentRepository {

    /**
     * Retrieves a document by its ID.
     *
     * @param documentId The ID of the document to be retrieved.
     * @return The resolved [Document] if found, null otherwise.
     */
    fun findById(documentId: Uuid): Document?

    /**
     * Retrieves all document entries.
     *
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    fun findAll(pageable: Pageable? = null): Page<Document>

    /**
     * Retrieves all document entries for a specific group.
     *
     * @param groupId The target group ID.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    fun findByGroupId(groupId: Uuid, pageable: Pageable?): Page<Document>

    /**
     * Retrieves all document entries for a specific owner.
     *
     * @param ownerId The target owner ID.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    fun findByOwnerId(ownerId: Uuid, pageable: Pageable?): Page<Document>

    /**
     * Retrieves all document entries matching the provided [filterSet].
     *
     * @param filterSet The [DocumentFilterSet] to be applied.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [Document] entries.
     */
    fun search(filterSet: DocumentFilterSet, pageable: Pageable? = null): Page<Document>

    /**
     * Creates a new document.
     *
     * @param request The document to be created.
     * @return The newly created [Document].
     */
    fun create(request: DocumentRequest): Document

    /**
     * Updates a document details.
     *
     * @param documentId The ID of the document to be updated.
     * @param request The new details for the document.
     * @return The updated [Document], or null if not found.
     */
    fun update(documentId: Uuid, request: DocumentRequest): Document?

    /**
     * Sets the cipher state of a document.
     *
     * @param documentId The ID of the document to be updated.
     * @param isCiphered Whether the document is ciphered.
     * @param storageName The name of the document in the storage.
     * @return The number of updated records.
     */
    fun setCipherState(documentId: Uuid, isCiphered: Boolean, storageName: String): Int

    /**
     * Deletes a document using the provided ID.
     *
     * @param documentId The ID of the document to be deleted.
     * @return The number of delete records.
     */
    fun delete(documentId: Uuid): Int

    /**
     * Deletes all document by group ID.
     *
     * @param groupId The group ID to be used for deletion.
     * @return The number of deleted records.
     */
    fun deleteByGroup(groupId: Uuid): Int

    /**
     * Deletes all document.
     *
     * @return The number of deleted records.
     */
    fun deleteAll(): Int

    /**
     * Retrieves the total count of document.
     *
     * @return The total count of existing records.
     */
    fun count(): Int
}
