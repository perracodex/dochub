/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.database.plugins

import io.ktor.server.application.*
import kdoc.core.env.Telemetry
import kdoc.database.schema.admin.actor.ActorTable
import kdoc.database.schema.admin.rbac.RbacFieldRuleTable
import kdoc.database.schema.admin.rbac.RbacRoleTable
import kdoc.database.schema.admin.rbac.RbacScopeRuleTable
import kdoc.database.schema.document.DocumentAuditTable
import kdoc.database.schema.document.DocumentTable

/**
 * Configures the custom [DbPlugin].
 *
 * This will set up and configure database, including the connection pool, and register
 * the database schema tables so that the ORM can interact with them.
 *
 * @see [DbPlugin]
 */
public fun Application.configureDatabase() {

    install(plugin = DbPlugin) {
        telemetryRegistry = Telemetry.registry

        // Default admin tables.
        tables.add(RbacFieldRuleTable)
        tables.add(RbacScopeRuleTable)
        tables.add(RbacRoleTable)
        tables.add(ActorTable)

        // Document tables.
        tables.add(DocumentTable)
        tables.add(DocumentAuditTable)
    }
}
