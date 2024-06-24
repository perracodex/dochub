/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.entity

import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.persistence.serializers.SUUID
import kotlinx.serialization.Serializable

/**
 * Represents the request to create/update a document.
 *
 * @property ownerId The ID of the actor who owns the document.
 * @property groupId The group to which the document belongs, allowing documents to be associated.
 * @property name The document's name.
 * @property type The [DocumentType] of the document.
 * @property description Optional description of the document.
 * @property location The document's storage location.
 * @property isCiphered Whether the document is ciphered.
 */
@Serializable
data class DocumentRequest(
    val ownerId: SUUID,
    val groupId: SUUID,
    val name: String,
    val type: DocumentType,
    val description: String?,
    val location: String,
    val isCiphered: Boolean
) {
    init {
        require(name.isNotBlank()) { "Document name must not be blank." }
        require(location.isNotBlank()) { "Document location must not be blank." }
    }
}
