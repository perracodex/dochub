/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.entity

import kdoc.base.database.schema.document.DocumentTable
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.persistence.entity.Meta
import kdoc.base.persistence.serializers.SUUID
import kdoc.document.service.DocumentService.Companion.PATH_SEPARATOR
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
 * @property detail The document's concrete details. Not to be exposed in an external client.
 * @property meta The document's metadata.
 */
@Serializable
data class DocumentEntity(
    val id: SUUID,
    val ownerId: SUUID,
    val groupId: SUUID,
    @Transient val detail: Detail = Detail(),
    val meta: Meta
) {
    /**
     * Concrete details that should not be exposed in an external client.
     *
     * @property type The [DocumentType] of the document.
     * @property description Optional description of the document.
     * @property originalName The original name of the document.
     * @property storageName The name of the document in storage.
     * @property location The document's storage location.
     * @property path The document's storage path, which is a combination of the location and the storage name.
     * @property isCiphered Whether the document is ciphered.
     * @property size The size of the document in bytes. Without encryption.
     */
    data class Detail(
        val type: DocumentType = DocumentType.GENERAL,
        val description: String? = null,
        val originalName: String = "",
        val storageName: String = "",
        val location: String = "",
        val path: String = "",
        val isCiphered: Boolean = true,
        val size: Long = 0L
    )

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
                detail = Detail(
                    type = row[DocumentTable.type],
                    description = row[DocumentTable.description],
                    originalName = row[DocumentTable.originalName],
                    storageName = row[DocumentTable.storageName],
                    location = row[DocumentTable.location],
                    path = "${row[DocumentTable.location]}$PATH_SEPARATOR${row[DocumentTable.storageName]}",
                    isCiphered = row[DocumentTable.isCiphered],
                    size = row[DocumentTable.size]
                ),
                meta = Meta.from(row = row, table = DocumentTable)
            )
        }
    }
}
