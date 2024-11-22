/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.database.schema.document

import kdoc.core.database.column.autoGenerate
import kdoc.core.database.column.enumerationById
import kdoc.core.database.column.kotlinUuid
import kdoc.core.database.schema.base.TimestampedTable
import kdoc.core.database.schema.document.type.DocumentType
import org.jetbrains.exposed.sql.Column
import kotlin.uuid.Uuid

/**
 * Database table definition for document information.
 */
public object DocumentTable : TimestampedTable(name = "document") {
    /** The record unique identifier. */
    public val id: Column<Uuid> = kotlinUuid(
        name = "document_id"
    ).autoGenerate()

    /** The ID of the actor who owns the document. */
    public val ownerId: Column<Uuid> = kotlinUuid(
        name = "owner_id"
    )

    /** The group to which the document belongs, allowing documents to be associated. */
    public val groupId: Column<Uuid> = kotlinUuid(
        name = "group_id"
    )

    /** The [DocumentType] of the document */
    public val type: Column<DocumentType> = enumerationById(
        name = "document_type_id",
        entries = DocumentType.entries
    )

    /** Optional description of the document. */
    public val description: Column<String?> = varchar(
        name = "description",
        length = 2048
    ).nullable()

    /** The original name of the document. */
    public val originalName: Column<String> = varchar(
        name = "original_name",
        length = 1024
    )

    /** The name of the document in storage. */
    public val storageName: Column<String> = varchar(
        name = "storage_name",
        length = 4098
    )

    /** The storage location of the document. */
    public val location: Column<String> = varchar(
        name = "location",
        length = 8192
    )

    /** Whether the document is ciphered. */
    public val isCiphered: Column<Boolean> = bool(
        name = "is_ciphered"
    )

    /** The size of the document in bytes. Without encryption. */
    public val size: Column<Long> = long(
        name = "document_size"
    )

    /**
     * The table's primary key.
     */
    override val primaryKey: PrimaryKey = PrimaryKey(
        firstColumn = id,
        name = "pk_document_id"
    )
}
