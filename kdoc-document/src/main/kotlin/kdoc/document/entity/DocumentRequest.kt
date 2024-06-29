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
 * @property type The [DocumentType] of the document.
 * @property description Optional description of the document.
 * @property originalName The original name of the document.
 * @property storageName The name of the document in storage.
 * @property location The document's storage path location.
 * @property isCiphered Whether the document is ciphered.
 * @property size The size of the document in bytes. Without encryption.
 */
@Serializable
data class DocumentRequest(
    val ownerId: SUUID,
    val groupId: SUUID,
    val type: DocumentType,
    val description: String?,
    val originalName: String,
    val storageName: String,
    val location: String,
    val isCiphered: Boolean,
    val size: Long
) {
    init {
        require(originalName.isNotBlank()) { "Document original name must not be blank." }
        require(storageName.isNotBlank()) { "Document storage name must not be blank." }
        require(location.isNotBlank()) { "Document location must not be blank." }
    }
}
