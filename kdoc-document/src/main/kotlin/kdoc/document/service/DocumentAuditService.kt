/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.service

import io.micrometer.core.instrument.Counter
import kdoc.base.env.SessionContext
import kdoc.base.plugins.appMicrometerRegistry
import kdoc.document.entity.DocumentAuditRequest
import kdoc.document.repository.IDocumentAuditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

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
        documentId: UUID? = null,
        groupId: UUID? = null,
        ownerId: UUID? = null,
        log: String? = null,
    ): Unit = withContext(Dispatchers.IO) {
        val request = DocumentAuditRequest(
            operation = operation,
            actorId = sessionContext.actorId,
            documentId = documentId,
            groupId = groupId,
            ownerId = ownerId,
            log = log
        )

        documentAuditRepository.create(documentAuditRequest = request)

        Counter.builder("documents: $operation")
            .description("Total number of uploaded files")
            .register(appMicrometerRegistry)
    }
}
