/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.entity

import kdoc.base.persistence.serializers.SUUID

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
internal data class DocumentAuditRequest(
    val operation: String,
    val actorId: SUUID? = null,
    val documentId: SUUID? = null,
    val groupId: SUUID? = null,
    val ownerId: SUUID? = null,
    val log: String? = null,
)