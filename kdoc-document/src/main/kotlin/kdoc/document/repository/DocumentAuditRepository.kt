/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import kdoc.base.database.schema.document.DocumentAuditTable
import kdoc.base.database.service.transactionWithSchema
import kdoc.base.env.SessionContext
import kdoc.document.entity.DocumentAuditRequest
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import java.util.*

/**
 * Implementation of the [IDocumentAuditRepository] interface.
 * Responsible for managing document audit data.
 */
internal class DocumentAuditRepository(
    private val sessionContext: SessionContext,
) : IDocumentAuditRepository {

    override fun create(documentAuditRequest: DocumentAuditRequest): UUID {
        return transactionWithSchema(schema = sessionContext.schema) {
            val newAuditId: UUID = DocumentAuditTable.insert { documentRow ->
                documentRow.mapDocumentRequest(documentAuditRequest = documentAuditRequest)
            } get DocumentAuditTable.id

            newAuditId
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [DocumentAuditRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.mapDocumentRequest(documentAuditRequest: DocumentAuditRequest) {
        this[DocumentAuditTable.operation] = documentAuditRequest.operation.trim()
        this[DocumentAuditTable.actorId] = documentAuditRequest.actorId
        this[DocumentAuditTable.documentId] = documentAuditRequest.documentId
        this[DocumentAuditTable.groupId] = documentAuditRequest.groupId
        this[DocumentAuditTable.ownerId] = documentAuditRequest.ownerId
        this[DocumentAuditTable.log] = documentAuditRequest.log?.trim()
    }
}
