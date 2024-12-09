/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.access.domain.actor.model

import kdochub.access.domain.rbac.model.role.RbacRole
import kdochub.database.model.Meta
import kdochub.database.schema.admin.actor.ActorTable
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.Uuid

/**
 * Represents a concrete Actor.
 * An Actor is a user with specific role and designated access to a set of concrete scopes.
 *
 * This class does not contain the Actor's password, so it can be cached in memory
 * without security concerns. If the password is needed, use [ActorCredentials] instead.
 *
 * @property id The Actor's unique id.
 * @property username The Actor's unique username.
 * @property role The associated role.
 * @property isLocked Whether the Actor is locked, so its role and associated rules are ignored, loosing all accesses.
 * @property meta The metadata of the record.
 *
 * @see [ActorCredentials]
 */
internal data class Actor private constructor(
    var id: Uuid,
    val username: String,
    val role: RbacRole,
    val isLocked: Boolean,
    val meta: Meta
) {
    companion object {
        /**
         * Maps a [ResultRow] to an [Actor] instance.
         *
         * @param row The [ResultRow] to map.
         * @param role The associated [RbacRole] instance.
         * @return The mapped [Actor] instance.
         */
        fun from(row: ResultRow, role: RbacRole): Actor {
            return Actor(
                id = row[ActorTable.id],
                username = row[ActorTable.username],
                role = role,
                isLocked = row[ActorTable.isLocked],
                meta = Meta.from(row = row, table = ActorTable)
            )
        }
    }
}
