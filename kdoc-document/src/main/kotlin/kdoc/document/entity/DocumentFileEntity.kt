/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.entity

import kdoc.base.database.schema.document.DocumentTable
import org.jetbrains.exposed.sql.ResultRow
import java.io.File

/**
 * Represents the physical file of a document.
 *
 * @property location The document's storage location.
 * @property originalName The original name of the document.
 * @property isCiphered Whether the document is ciphered.
 * @property file The document file reference.
 */
data class DocumentFileEntity(
    val location: String,
    val originalName: String,
    val isCiphered: Boolean,
    val file: File
) {
    companion object {
        /**
         * Maps a [ResultRow] to a [DocumentFileEntity] instance.
         *
         * @param row The [ResultRow] to map.
         * @return The mapped [DocumentFileEntity] instance.
         */
        fun from(row: ResultRow): DocumentFileEntity {
            return DocumentFileEntity(
                location = row[DocumentTable.location],
                originalName = row[DocumentTable.name],
                isCiphered = row[DocumentTable.isCiphered],
                file = File(row[DocumentTable.location])
            )
        }
    }
}
