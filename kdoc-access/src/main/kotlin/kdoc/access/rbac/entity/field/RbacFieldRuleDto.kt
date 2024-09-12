/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.entity.field

import kdoc.base.database.schema.admin.rbac.RbacFieldRuleTable
import kdoc.base.database.schema.admin.rbac.types.RbacAccessLevel
import kdoc.base.database.schema.admin.rbac.types.RbacScope
import kdoc.base.persistence.entity.Meta
import kdoc.base.persistence.serializers.UuidS
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

/**
 * Represents a single RBAC field level rule.
 *
 * @property id The unique id of the field rule record.
 * @property scopeRuleId The associated parent [RbacScope] id.
 * @property fieldName The name of the field being targeted.
 * @property accessLevel The field [RbacAccessLevel].
 * @property meta The metadata of the record.
 */
@Serializable
public data class RbacFieldRuleDto(
    val id: UuidS,
    val scopeRuleId: UuidS,
    val fieldName: String,
    val accessLevel: RbacAccessLevel,
    val meta: Meta
) {
    internal companion object {
        /**
         * Maps a [ResultRow] to a [RbacFieldRuleDto] instance.
         *
         * @param row The [ResultRow] to map.
         * @return The mapped [RbacFieldRuleDto] instance.
         */
        fun from(row: ResultRow): RbacFieldRuleDto {
            return RbacFieldRuleDto(
                id = row[RbacFieldRuleTable.id],
                scopeRuleId = row[RbacFieldRuleTable.scopeRuleId],
                fieldName = row[RbacFieldRuleTable.fieldName],
                accessLevel = row[RbacFieldRuleTable.accessLevel],
                meta = Meta.from(row = row, table = RbacFieldRuleTable)
            )
        }
    }
}
