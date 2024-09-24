/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.paginate
import kdoc.core.database.schema.document.DocumentTable
import kdoc.core.database.utils.transaction
import kdoc.core.env.SessionContext
import kdoc.document.model.Document
import kdoc.document.model.DocumentFilterSet
import kdoc.document.model.DocumentRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import kotlin.uuid.Uuid

/**
 * Implementation of the [IDocumentRepository] interface.
 * Responsible for managing document data.
 */
internal class DocumentRepository(
    private val sessionContext: SessionContext
) : IDocumentRepository {

    override fun findById(documentId: Uuid): Document? {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.selectAll().where {
                DocumentTable.id eq documentId
            }.singleOrNull()?.let { resultRow ->
                Document.from(row = resultRow)
            }
        }
    }

    override fun findByOwnerId(ownerId: Uuid, pageable: Pageable?): Page<Document> {
        return fetchByCondition(
            condition = { DocumentTable.ownerId eq ownerId },
            pageable = pageable
        )
    }

    override fun findByGroupId(groupId: Uuid, pageable: Pageable?): Page<Document> {
        return fetchByCondition(
            condition = { DocumentTable.groupId eq groupId },
            pageable = pageable
        )
    }

    /**
     * Common method to fetch documents with a specific condition.
     */
    private fun fetchByCondition(
        condition: SqlExpressionBuilder.() -> Op<Boolean>,
        pageable: Pageable?
    ): Page<Document> {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.selectAll()
                .andWhere(condition)
                .paginate(pageable = pageable, transform = Document)
        }
    }

    override fun findAll(pageable: Pageable?): Page<Document> {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.selectAll().paginate(pageable = pageable, transform = Document)
        }
    }

    override fun search(filterSet: DocumentFilterSet, pageable: Pageable?): Page<Document> {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.selectAll().apply {
                // Apply filters dynamically based on the presence of criteria in filterSet.
                // Using lowerCase() to make the search case-insensitive.
                // This could be removed if the database is configured to use a case-insensitive collation.

                filterSet.id?.let { documentId ->
                    andWhere {
                        DocumentTable.id eq documentId
                    }
                }
                filterSet.ownerId?.let { ownerId ->
                    andWhere {
                        DocumentTable.ownerId eq ownerId
                    }
                }
                filterSet.groupId?.let { groupId ->
                    andWhere {
                        DocumentTable.groupId eq groupId
                    }
                }
                filterSet.name?.let { name ->
                    andWhere {
                        DocumentTable.originalName.lowerCase() like "%${name.trim().lowercase()}%"
                    }
                }
                filterSet.description?.let { description ->
                    andWhere {
                        DocumentTable.description.lowerCase() like "%${description.trim().lowercase()}%"
                    }
                }
                filterSet.type?.let { typeList ->
                    if (typeList.isNotEmpty()) {
                        andWhere {
                            DocumentTable.type inList typeList
                        }
                    }
                }
            }.paginate(pageable = pageable, transform = Document)
        }
    }

    override fun create(request: DocumentRequest): Document {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.insert { statement ->
                statement.toStatement(request = request)
            }[DocumentTable.id].let { documentId ->
                findById(documentId = documentId)
                    ?: throw IllegalStateException("Failed to create document.")
            }
        }
    }

    override fun update(documentId: Uuid, request: DocumentRequest): Document? {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.update(
                where = {
                    DocumentTable.id eq documentId
                }
            ) { statement ->
                statement.toStatement(request = request)
            }.takeIf { it > 0 }?.let {
                findById(documentId = documentId)
            }
        }
    }

    override fun setCipherState(documentId: Uuid, isCiphered: Boolean, storageName: String): Int {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.update(
                where = {
                    DocumentTable.id eq documentId
                }
            ) { statement ->
                statement[DocumentTable.isCiphered] = isCiphered
                statement[DocumentTable.storageName] = storageName
            }
        }
    }

    override fun delete(documentId: Uuid): Int {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.deleteWhere {
                id eq documentId
            }
        }
    }

    override fun deleteByGroup(groupId: Uuid): Int {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.deleteWhere {
                DocumentTable.groupId eq groupId
            }
        }
    }

    override fun deleteAll(): Int {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.deleteAll()
        }
    }

    override fun count(): Int {
        return transaction(sessionContext = sessionContext) {
            DocumentTable.selectAll().count().toInt()
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [DocumentRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.toStatement(request: DocumentRequest) {
        this[DocumentTable.ownerId] = request.ownerId
        this[DocumentTable.groupId] = request.groupId
        this[DocumentTable.type] = request.type
        this[DocumentTable.description] = request.description?.trim()
        this[DocumentTable.originalName] = request.originalName.trim()
        this[DocumentTable.storageName] = request.storageName.trim()
        this[DocumentTable.location] = request.location.trim()
        this[DocumentTable.isCiphered] = request.isCiphered
        this[DocumentTable.size] = request.size
    }
}
