/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service

import kdoc.core.env.SessionContext
import kdoc.document.model.DocumentAuditLogRequest
import kdoc.document.repository.IDocumentAuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

/**
 * Document audit service, where all the documents audit logic should be defined.
 */
internal class DocumentAuditService(
    private val sessionContext: SessionContext,
    private val documentAuditRepository: IDocumentAuditRepository
) {
    /**
     * Audit a document operation.
     *
     * @property operation The operation that was performed.
     * @property documentId The unique identifier of the document being audited.
     * @property groupId The unique identifier of the group being audited.
     * @property ownerId The unique identifier of the owner of a document being audited.
     * @property log Optional log information.
     */
    suspend fun audit(
        operation: String,
        documentId: Uuid? = null,
        groupId: Uuid? = null,
        ownerId: Uuid? = null,
        log: String? = null,
    ): Unit = withContext(Dispatchers.IO) {
        val request = DocumentAuditLogRequest(
            operation = operation,
            actorId = sessionContext.actorId,
            documentId = documentId,
            groupId = groupId,
            ownerId = ownerId,
            log = log
        )

        documentAuditRepository.create(request = request)
    }
}
