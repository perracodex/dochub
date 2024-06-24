/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import kdoc.base.persistence.pagination.Page
import kdoc.base.persistence.pagination.Pageable
import kdoc.document.entity.DocumentEntity
import kdoc.document.entity.DocumentFilterSet
import kdoc.document.entity.DocumentFileEntity
import kdoc.document.entity.DocumentRequest
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import java.util.*

/**
 * Responsible for managing documents data.
 */
internal interface IDocumentRepository {

    /**
     * Retrieves a document by its ID.
     *
     * @param documentId The ID of the document to be retrieved.
     * @return The resolved [DocumentEntity] if found, null otherwise.
     */
    fun findById(documentId: UUID): DocumentEntity?

    /**
     * Retrieves all document entities.
     *
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [DocumentEntity] entries.
     */
    fun findAll(pageable: Pageable? = null): Page<DocumentEntity>

    /**
     * Retrieves all document entities matching the provided [condition].
     *
     * @param condition The condition to be applied.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [DocumentEntity] entries.
     */
    fun fetch(condition: SqlExpressionBuilder.() -> Op<Boolean>, pageable: Pageable?): Page<DocumentEntity>

    /**
     * Retrieves all document entities matching the provided [filterSet].
     *
     * @param filterSet The [DocumentFilterSet] to be applied.
     * @param pageable The pagination options to be applied, or null for a single all-in-one page.
     * @return List of [DocumentEntity] entries.
     */
    fun search(filterSet: DocumentFilterSet, pageable: Pageable? = null): Page<DocumentEntity>

    /**
     * Creates a new document.
     *
     * @param documentRequest The document to be created.
     * @return The ID of the created document.
     */
    fun create(documentRequest: DocumentRequest): UUID

    /**
     * Updates a document details.
     *
     * @param documentId The ID of the document to be updated.
     * @param documentRequest The new details for the document.
     * @return The number of updated records.
     */
    fun update(documentId: UUID, documentRequest: DocumentRequest): Int

    /**
     * Sets the cipher state of a document.
     * This implies also to update the location of the document, as its name will change.
     *
     * @param documentId The ID of the document to be updated.
     * @param isCiphered Whether the document is ciphered.
     * @param location The location of the document, which will be different if the cipher state is changed.
     * @return The number of updated records.
     */
    fun setCipherState(documentId: UUID, isCiphered: Boolean, location: String): Int

    /**
     * Deletes a document using the provided ID.
     *
     * @param documentId The ID of the document to be deleted.
     * @return The number of delete records.
     */
    fun delete(documentId: UUID): Int

    /**
     * Deletes all document by group ID.
     *
     * @param groupId The group ID to be used for deletion.
     * @return The number of deleted records.
     */
    fun deleteByGroup(groupId: UUID): Int

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

    /**
     * Retrieves a reference to the document storage file.
     *
     * @param documentId The ID of the document to be retrieved.
     * @return The [DocumentFileEntity] reference if found, null otherwise.
     */
    fun getStorageFile(documentId: UUID): DocumentFileEntity?
}
