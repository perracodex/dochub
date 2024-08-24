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
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Implementation of the [IDocumentAuditRepository] interface.
 * Responsible for managing document audit data.
 */
internal class DocumentAuditRepository(
    private val sessionContext: SessionContext,
) : IDocumentAuditRepository {

    override fun create(documentAuditRequest: DocumentAuditRequest): Uuid {
        return transactionWithSchema(schema = sessionContext.schema) {
            val newAuditId: Uuid = (DocumentAuditTable.insert { documentRow ->
                documentRow.mapDocumentRequest(documentAuditRequest = documentAuditRequest)
            } get DocumentAuditTable.id).toKotlinUuid()

            newAuditId
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [DocumentAuditRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.mapDocumentRequest(documentAuditRequest: DocumentAuditRequest) {
        this[DocumentAuditTable.operation] = documentAuditRequest.operation.trim()
        this[DocumentAuditTable.actorId] = documentAuditRequest.actorId?.toJavaUuid()
        this[DocumentAuditTable.documentId] = documentAuditRequest.documentId?.toJavaUuid()
        this[DocumentAuditTable.groupId] = documentAuditRequest.groupId?.toJavaUuid()
        this[DocumentAuditTable.ownerId] = documentAuditRequest.ownerId?.toJavaUuid()
        this[DocumentAuditTable.log] = documentAuditRequest.log?.trim()
    }
}
