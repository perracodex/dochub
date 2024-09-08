/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.routing

import io.ktor.server.http.content.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.plugin.withRbac
import kdoc.access.rbac.routing.admin.rbacAdminLoadRoute
import kdoc.access.rbac.routing.admin.rbacAdminUpdateRoute
import kdoc.access.rbac.routing.login.rbacLoginRoute
import kdoc.access.rbac.routing.login.rbacLogoutRoute
import kdoc.base.database.schema.admin.rbac.types.RbacAccessLevel
import kdoc.base.database.schema.admin.rbac.types.RbacScope
import kdoc.base.plugins.RateLimitScope

/**
 * Contains the RBAC endpoints.
 */
@OptIn(RbacAPI::class)
public fun Route.rbacRoute() {

    // Configures the server to serve CSS files located in the 'rbac' resources folder,
    // necessary for styling the RBAC Admin panel built with HTML DSL.
    staticResources(remotePath = "/static-rbac", basePackage = "rbac")

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
        rbacLoginRoute()
        rbacLogoutRoute()

        withRbac(scope = RbacScope.RBAC_ADMIN, accessLevel = RbacAccessLevel.VIEW) {
            rbacAdminLoadRoute()
            rbacAdminUpdateRoute()
        }
    }
}
