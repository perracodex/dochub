/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import kdoc.core.context.SessionContext
import kdoc.database.schema.document.DocumentAuditTable
import kdoc.database.util.transaction
import kdoc.document.model.DocumentAuditLogRequest
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import kotlin.uuid.Uuid

/**
 * Implementation of the [IDocumentAuditRepository] interface.
 * Responsible for managing document audit data.
 */
internal class DocumentAuditRepository(
    private val sessionContext: SessionContext,
) : IDocumentAuditRepository {

    override fun create(request: DocumentAuditLogRequest): Uuid {
        return transaction(sessionContext = sessionContext) {
            val newAuditId: Uuid = DocumentAuditTable.insert { statement ->
                statement.toStatement(request = request)
            } get DocumentAuditTable.id

            newAuditId
        }
    }

    /**
     * Populates an SQL [UpdateBuilder] with data from an [DocumentAuditLogRequest] instance,
     * so that it can be used to update or create a database record.
     */
    private fun UpdateBuilder<Int>.toStatement(request: DocumentAuditLogRequest) {
        this[DocumentAuditTable.operation] = request.operation.trim()
        this[DocumentAuditTable.actorId] = request.actorId
        this[DocumentAuditTable.documentId] = request.documentId
        this[DocumentAuditTable.groupId] = request.groupId
        this[DocumentAuditTable.ownerId] = request.ownerId
        this[DocumentAuditTable.log] = request.log?.trim()
    }
}
