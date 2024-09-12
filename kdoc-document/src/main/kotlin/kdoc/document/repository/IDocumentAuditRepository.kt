/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import kdoc.document.model.DocumentAuditLogRequest
import kotlin.uuid.Uuid

/**
 * Responsible for managing documents audit data.
 */
internal interface IDocumentAuditRepository {

    /**
     * Creates a new audit log entry.
     *
     * @param request The new audit log entry data.
     * @return The ID of the created audit log entry.
     */
    fun create(request: DocumentAuditLogRequest): Uuid
}
