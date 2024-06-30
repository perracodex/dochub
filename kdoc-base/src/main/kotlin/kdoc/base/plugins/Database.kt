/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.plugins

import io.ktor.server.application.*
import kdoc.base.database.plugin.DbPlugin
import kdoc.base.database.schema.admin.actor.ActorTable
import kdoc.base.database.schema.admin.rbac.RbacFieldRuleTable
import kdoc.base.database.schema.admin.rbac.RbacRoleTable
import kdoc.base.database.schema.admin.rbac.RbacScopeRuleTable
import kdoc.base.database.schema.document.DocumentAuditTable
import kdoc.base.database.schema.document.DocumentTable
import kdoc.base.env.MetricsRegistry

/**
 * Configures the custom [DbPlugin].
 *
 * This will set up and configure database, including the connection pool, and register
 * the database schema tables so that the ORM can interact with them.
 *
 * @see DbPlugin
 */
fun Application.configureDatabase() {

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
