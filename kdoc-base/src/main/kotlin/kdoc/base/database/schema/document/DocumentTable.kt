/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.database.schema.document

import kdoc.base.database.schema.base.TimestampedTable
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.persistence.utils.enumerationById
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.util.*

/**
 * Database table definition for document information.
 */
object DocumentTable : TimestampedTable(name = "document") {
    /** The record unique identifier. */
    val id: Column<UUID> = uuid(
        name = "document_id"
    ).autoGenerate()

    /** The ID of the actor who owns the document. */
    val ownerId: Column<UUID> = uuid(
        name = "owner_id"
    )

    /** The group to which the document belongs, allowing documents to be associated. */
    val groupId: Column<UUID> = uuid(
        name = "group_id"
    )

    /** The name of the document. */
    val name: Column<String> = varchar(
        name = "document_name",
        length = 512
    )

    /** The [DocumentType] of the document */
    val type: Column<DocumentType> = enumerationById(
        name = "document_type",
        fromId = DocumentType::fromId
    )

    /** Optional description of the document. */
    val description: Column<String?> = varchar(
        name = "description",
        length = 2048
    ).nullable()

    /** The storage location of the document. */
    val location: Column<String> = varchar(
        name = "location",
        length = 4098
    )

    /** Whether the document is ciphered. */
    val isCiphered: Column<Boolean> = bool(
        name = "is_ciphered"
    )

    override val primaryKey: Table.PrimaryKey = PrimaryKey(
        firstColumn = id,
        name = "pk_document_id"
    )
}
