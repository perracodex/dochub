/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.routing

import io.ktor.server.http.content.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.routing.admin.rbacAdminRoute
import kdoc.access.rbac.routing.login.rbacLoginRoute
import kdoc.access.rbac.routing.login.rbacLogoutRoute
import kdoc.base.plugins.RateLimitScope

/**
 * Contains the RBAC endpoints.
 */
@OptIn(RbacAPI::class)
fun Route.rbacRoute() {

    // Required so the HTML for can find its respective CSS file.
    staticResources(remotePath = "/static-rbac", basePackage = "rbac")

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
        rbacLoginRoute()
        rbacLogoutRoute()
        rbacAdminRoute()
    }
}
