/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.model

import kotlin.uuid.Uuid

/**
 * Represents a request to create an audit log entry for a document.
 *
 * @property operation The operation that was performed.
 * @property actorId The unique identifier of the actor that performed the operation.
 * @property documentId The unique identifier of the document being audited.
 * @property groupId The unique identifier of the group being audited.
 * @property ownerId The unique identifier of the owner of a document being audited.
 * @property log Additional log information.
 */
internal data class DocumentAuditLogRequest(
    val operation: String,
    val actorId: Uuid? = null,
    val documentId: Uuid? = null,
    val groupId: Uuid? = null,
    val ownerId: Uuid? = null,
    val log: String? = null,
)