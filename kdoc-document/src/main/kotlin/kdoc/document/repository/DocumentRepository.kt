/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import kdoc.base.database.schema.document.DocumentTable
import kdoc.base.database.service.transactionWithSchema
import kdoc.base.env.SessionContext
import kdoc.base.persistence.pagination.Page
import kdoc.base.persistence.pagination.Pageable
import kdoc.base.persistence.pagination.applyPagination
import kdoc.document.entity.DocumentEntity
import kdoc.document.entity.DocumentFilterSet
import kdoc.document.entity.DocumentRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Implementation of the [IDocumentRepository] interface.
 * Responsible for managing document data.
 */
internal class DocumentRepository(
    private val sessionContext: SessionContext
) : IDocumentRepository {

    override fun findById(documentId: Uuid): DocumentEntity? {
        return transactionWithSchema(schema = sessionContext.schema) {
            DocumentTable.selectAll().where {
                DocumentTable.id eq documentId.toJavaUuid()
            }.singleOrNull()?.let { resultRow ->
                DocumentEntity.from(row = resultRow)
            }
        }
    }

    override fun findByOwnerId(ownerId: Uuid, pageable: Pageable?): Page<DocumentEntity> {
        return fetchByCondition(
            condition = { DocumentTable.ownerId eq ownerId.toJavaUuid() },
            pageable = pageable
        )
    }

    override fun findByGroupId(groupId: Uuid, pageable: Pageable?): Page<DocumentEntity> {
        return fetchByCondition(
            condition = { DocumentTable.groupId eq groupId.toJavaUuid() },
            pageable = pageable
        )
    }

    /**
     * Common method to fetch documents with a specific condition.
     */
    private fun fetchByCondition(
        condition: SqlExpressionBuilder.() -> Op<Boolean>,
        pageable: Pageable?
    ): Page<DocumentEntity> {
        return transactionWithSchema(schema = sessionContext.schema) {
            // Start a query with the target condition.
            val query: Query = DocumentTable.selectAll().andWhere(condition)

            // Count total elements that match the condition for accurate pagination information.
            val totalElements: Int = query.count().toInt()

            // Fetch the paginated content.
            val content: List<DocumentEntity> = query
                .applyPagination(pageable = pageable)
                .map { resultRow ->
                    DocumentEntity.from(row = resultRow)
                }

            Page.build(
                content = content,
                totalElements = totalElements,
                pageable = pageable
            )
        }
    }

    override fun findAll(pageable: Pageable?): Page<DocumentEntity> {
        return transactionWithSchema(schema = sessionContext.schema) {
            val totalElements: Int = DocumentTable.selectAll().count().toInt()

            val content: List<DocumentEntity> = DocumentTable.selectAll()
                .applyPagination(pageable = pageable)
                .map { resultRow ->
                    DocumentEntity.from(row = resultRow)
                }

            Page.build(
                content = content,
                totalElements = totalElements,
                pageable = pageable
            )
        }
    }

    override fun search(filterSet: DocumentFilterSet, pageable: Pageable?): Page<DocumentEntity> {
        return transactionWithSchema(schema = sessionContext.schema) {
            // Start with a base query selecting all records.
            val query: Query = DocumentTable.selectAll()

            // Apply filters dynamically based on the presence of criteria in filterSet.
            // Using lowerCase() to make the search case-insensitive.
            // This could be removed if the database is configured to use a case-insensitive collation.
            filterSet.id?.let { documentId ->
                query.andWhere {
                    DocumentTable.id eq documentId.toJavaUuid()
                }
            }
            filterSet.ownerId?.let { ownerId ->
                query.andWhere {
                    DocumentTable.ownerId eq ownerId.toJavaUuid()
                }
            }
            filterSet.groupId?.let { groupId ->
                query.andWhere {
                    DocumentTable.groupId eq groupId.toJavaUuid()
                }
            }
            filterSet.name?.let { name ->
                query.andWhere {
                    DocumentTable.originalName.lowerCase() like "%${name.trim().lowercase()}%"
                }
            }
            filterSet.description?.let { description ->
                query.andWhere {
                    DocumentTable.description.lowerCase() like "%${description.trim().lowercase()}%"
                }
            }
            filterSet.type?.let { typeList ->
                if (typeList.isNotEmpty()) {
                    query.andWhere {
                        DocumentTable.type inList typeList
                    }
                }
            }

            // Count total elements after applying filters.
            val totalFilteredElements: Int = query.count().toInt()

            val content: List<DocumentEntity> = query
                .applyPagination(pageable = pageable)
                .map { resultRow ->
                    DocumentEntity.from(row = resultRow)
                }

            Page.build(
                content = content,
                totalElements = totalFilteredElements,
                pageable = pageable
            )
        }
    }

    override fun create(documentRequest: DocumentRequest): Uuid {
        return transactionWithSchema(schema = sessionContext.schema) {
            val newDocumentId: Uuid = (DocumentTable.insert { documentRow ->
                documentRow.mapDocumentRequest(documentRequest = documentRequest)
            } get DocumentTable.id).toKotlinUuid()

            newDocumentId
        }
    }

    override fun update(documentId: Uuid, documentRequest: DocumentRequest): Int {
        return transactionWithSchema(schema = sessionContext.schema) {
            val updateCount: Int = DocumentTable.update(
                where = {
                    DocumentTable.id eq documentId.toJavaUuid()
                }
            ) { documentRow ->
                documentRow.mapDocumentRequest(documentRequest = documentRequest)
            }

            updateCount
        }
    }

    override fun setCipherState(documentId: Uuid, isCiphered: Boolean, storageName: String): Int {
        return transactionWithSchema(schema = sessionContext.schema) {
            DocumentTable.update(
                where = {
                    DocumentTable.id eq documentId.toJavaUuid()
                }
            ) {
                it[DocumentTable.isCiphered] = isCiphered
                it[DocumentTable.storageName] = storageName
            }
        }
    }

    override fun delete(documentId: Uuid): Int {
        return transactionWithSchema(schema = sessionContext.schema) {
            DocumentTable.deleteWhere {
                id eq documentId.toJavaUuid()
            }
        }
    }

    override fun deleteByGroup(groupId: Uuid): Int {
        return transactionWithSchema(schema = sessionContext.schema) {
            DocumentTable.deleteWhere {
                DocumentTable.groupId eq groupId.toJavaUuid()
            }
        }
    }

    override fun deleteAll(): Int {
        return transactionWithSchema(schema = sessionContext.schema) {
            DocumentTable.deleteAll()
        }
    }

    override fun count(): Int {
        return transactionWithSchema(schema = sessionContext.schema) {
            DocumentTable.selectAll().count().toInt()
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [DocumentRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.mapDocumentRequest(documentRequest: DocumentRequest) {
        this[DocumentTable.ownerId] = documentRequest.ownerId.toJavaUuid()
        this[DocumentTable.groupId] = documentRequest.groupId.toJavaUuid()
        this[DocumentTable.type] = documentRequest.type
        this[DocumentTable.description] = documentRequest.description?.trim()
        this[DocumentTable.originalName] = documentRequest.originalName.trim()
        this[DocumentTable.storageName] = documentRequest.storageName.trim()
        this[DocumentTable.location] = documentRequest.location.trim()
        this[DocumentTable.isCiphered] = documentRequest.isCiphered
        this[DocumentTable.size] = documentRequest.size
    }
}
