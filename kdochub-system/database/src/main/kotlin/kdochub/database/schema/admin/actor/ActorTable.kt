/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.database.schema.admin.actor

import kdochub.core.security.util.EncryptionUtils
import kdochub.database.column.autoGenerate
import kdochub.database.column.kotlinUuid
import kdochub.database.schema.admin.rbac.RbacRoleTable
import kdochub.database.schema.base.TimestampedTable
import org.jetbrains.exposed.crypt.Encryptor
import org.jetbrains.exposed.crypt.encryptedVarchar
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import kotlin.uuid.Uuid

/**
 * Database table definition holding Actors.
 * An Actor is a user with specific role and designated access to a set of concrete scopes.
 */
public object ActorTable : TimestampedTable(name = "actor") {
    private val encryptor: Encryptor = EncryptionUtils.getEncryptor(type = EncryptionUtils.Type.AT_REST)

    /**
     * The unique id of the Actor record.
     */
    public val id: Column<Uuid> = kotlinUuid(
        name = "actor_id"
    ).autoGenerate()

    /**
     * The actor's unique username.
     */
    public val username: Column<String> = varchar(
        name = "username",
        length = 16
    ).uniqueIndex(
        customIndexName = "uq_actor__username"
    )

    /**
     * The actor's encrypted password.
     */
    public val password: Column<String> = encryptedVarchar(
        name = "password",
        cipherTextLength = encryptor.maxColLength(inputByteSize = 128),
        encryptor = encryptor
    )

    /**
     * The associated [RbacRoleTable] id.
     */
    public val roleId: Column<Uuid> = kotlinUuid(
        name = "role_id"
    ).references(
        fkName = "fk_rbac_role__role_id",
        ref = RbacRoleTable.id,
        onDelete = ReferenceOption.RESTRICT,
        onUpdate = ReferenceOption.RESTRICT
    )

    /**
     * Whether the Actor is locked and therefore has full restricted access.
     */
    public val isLocked: Column<Boolean> = bool(
        name = "is_locked"
    )

    override val primaryKey: PrimaryKey = PrimaryKey(
        firstColumn = id,
        name = "pk_actor_id"
    )
}
