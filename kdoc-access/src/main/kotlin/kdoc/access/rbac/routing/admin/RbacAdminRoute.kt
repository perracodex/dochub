/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.routing.admin

import io.ktor.server.routing.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.plugin.withRbac
import kdoc.base.database.schema.admin.rbac.types.RbacAccessLevel
import kdoc.base.database.schema.admin.rbac.types.RbacScope

/**
 * Defines all the RBAC admin routes.
 */
@OptIn(RbacAPI::class)
internal fun Route.rbacAdminRoute() {
    withRbac(scope = RbacScope.RBAC_ADMIN, accessLevel = RbacAccessLevel.VIEW) {
        rbacAdminRouteGet()
        rbacAdminRoutePost()
    }
}
