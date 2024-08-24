/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.repository

import kdoc.document.entity.DocumentAuditRequest
import kotlin.uuid.Uuid

/**
 * Responsible for managing documents audit data.
 */
internal interface IDocumentAuditRepository {

    /**
     * Creates a new audit log entry.
     *
     * @param documentAuditRequest The new audit log entry data.
     * @return The ID of the created audit log entry.
     */
    fun create(documentAuditRequest: DocumentAuditRequest): Uuid
}
