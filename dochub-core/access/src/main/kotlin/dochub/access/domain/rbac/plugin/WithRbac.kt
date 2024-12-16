/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.domain.rbac.plugin

import dochub.access.domain.rbac.annotation.RbacApi
import dochub.base.settings.AppSettings
import dochub.database.schema.admin.rbac.type.RbacAccessLevel
import dochub.database.schema.admin.rbac.type.RbacScope
import io.ktor.server.routing.*

/**
 * Extension function designed to apply RBAC authorizations to Ktor routes.
 *
 * @param scope The RBAC scope associated with the route, defining the scope of access control.
 * @param accessLevel The RBAC access level required for accessing the route, defining the degree of access control.
 * @param build The lambda function defining the route's handling logic that must adhere to the RBAC constraints.
 * @return The created Route object configured with RBAC constraints.
 */
@OptIn(RbacApi::class)
public fun Route.withRbac(scope: RbacScope, accessLevel: RbacAccessLevel, build: Route.() -> Unit): Route {
    return if (AppSettings.security.rbac.isEnabled) {
        rbacAuthorizedRoute(scope = scope, accessLevel = accessLevel, build = build)
    } else {
        this.apply(build)
    }
}
