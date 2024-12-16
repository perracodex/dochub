/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.domain.rbac.api

import dochub.access.domain.rbac.annotation.RbacApi
import dochub.access.domain.rbac.api.dashboard.rbacDashboardLoadRoute
import dochub.access.domain.rbac.api.dashboard.rbacDashboardUpdateRoute
import dochub.access.domain.rbac.api.login.rbacLoginAccessRoute
import dochub.access.domain.rbac.api.login.rbacLoginSubmissionRoute
import dochub.access.domain.rbac.api.login.rbacLogoutRoute
import dochub.access.domain.rbac.plugin.withRbac
import dochub.base.plugins.RateLimitScope
import dochub.database.schema.admin.rbac.type.RbacAccessLevel
import dochub.database.schema.admin.rbac.type.RbacScope
import io.ktor.server.http.content.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*

/**
 * Contains the RBAC endpoints.
 *
 * These include the login and logout routes, as well as the dashboard routes.
 */
@OptIn(RbacApi::class)
public fun Route.rbacRoutes() {

    // Configures the server to serve CSS files located in the 'rbac' resources folder,
    // necessary for styling the RBAC dashboard built with HTML DSL.
    staticResources(remotePath = "/static-rbac", basePackage = "rbac")

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
        rbacLoginAccessRoute()
        rbacLoginSubmissionRoute()
        rbacLogoutRoute()

        withRbac(scope = RbacScope.RBAC_DASHBOARD, accessLevel = RbacAccessLevel.VIEW) {
            rbacDashboardLoadRoute()
            rbacDashboardUpdateRoute()
        }
    }
}
