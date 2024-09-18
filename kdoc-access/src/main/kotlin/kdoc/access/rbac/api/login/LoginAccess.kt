/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.api.login

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.service.RbacDashboardManager
import kdoc.access.rbac.view.RbacDashboardView
import kdoc.access.rbac.view.RbacLoginView
import kdoc.base.env.CallContext

/**
 * Manages access to the RBAC login page. If a valid session is already exists, the actor
 * is directly redirected to the dashboard. Otherwise, any existing session cookies are
 * cleared and the login page is presented.
 */
@RbacAPI
internal fun Route.rbacLoginAccessRoute() {
    /**
     * Redirects actors to the dashboard if they have an existing session,
     * or to the login page if no valid session is found.
     * @OpenAPITag RBAC
     */
    get("rbac/login") {
        RbacDashboardManager.getCallContext(call = call)?.let {
            call.respondRedirect(url = RbacDashboardView.RBAC_DASHBOARD_PATH)
        } ?: run {
            call.sessions.clear(name = CallContext.SESSION_NAME)
            call.respondHtml(status = HttpStatusCode.OK) {
                RbacLoginView.build(html = this)
            }
        }
    }
}
