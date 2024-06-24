/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.entity

import kdoc.base.database.schema.document.DocumentTable
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.persistence.entity.Meta
import kdoc.base.persistence.serializers.SUUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.ResultRow

/**
 * Represents the entity for a document.
 * To protect potentially sensitive information, some fields are marked as [Transient],
 * meaning that they are not serialized when converting the entity to JSON.
 *
 * @property id The document's unique id.
 * @property ownerId The ID of the actor who owns the document.
 * @property groupId The group to which the document belongs, allowing documents to be associated.
 * @property name The document's name.
 * @property type The [DocumentType] of the document.
 * @property description Optional description of the document.
 * @property isCiphered Whether the document is ciphered.
 * @property meta The document's metadata.
 */
@Serializable
data class DocumentEntity(
    val id: SUUID,
    val ownerId: SUUID,
    val groupId: SUUID,
    @Transient val name: String = "",
    @Transient val type: DocumentType = DocumentType.GENERAL,
    @Transient val description: String? = null,
    @Transient val isCiphered: Boolean = false,
    val meta: Meta
) {
    companion object {
        /**
         * Maps a [ResultRow] to a [DocumentEntity] instance.
         *
         * @param row The [ResultRow] to map.
         * @return The mapped [DocumentEntity] instance.
         */
        fun from(row: ResultRow): DocumentEntity {
            return DocumentEntity(
                id = row[DocumentTable.id],
                ownerId = row[DocumentTable.ownerId],
                groupId = row[DocumentTable.groupId],
                name = row[DocumentTable.name],
                type = row[DocumentTable.type],
                description = row[DocumentTable.description],
                isCiphered = row[DocumentTable.isCiphered],
                meta = Meta.from(row = row, table = DocumentTable)
            )
        }
    }
}
