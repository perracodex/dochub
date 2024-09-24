/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.plugins

import io.ktor.server.application.*
import kdoc.core.database.plugin.DbPlugin
import kdoc.core.database.schema.admin.actor.ActorTable
import kdoc.core.database.schema.admin.rbac.RbacFieldRuleTable
import kdoc.core.database.schema.admin.rbac.RbacRoleTable
import kdoc.core.database.schema.admin.rbac.RbacScopeRuleTable
import kdoc.core.database.schema.document.DocumentAuditTable
import kdoc.core.database.schema.document.DocumentTable
import kdoc.core.env.MetricsRegistry

/**
 * Configures the custom [DbPlugin].
 *
 * This will set up and configure database, including the connection pool, and register
 * the database schema tables so that the ORM can interact with them.
 *
 * @see DbPlugin
 */
public fun Application.configureDatabase() {

    install(plugin = DbPlugin) {
        micrometerRegistry = MetricsRegistry.registry

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
