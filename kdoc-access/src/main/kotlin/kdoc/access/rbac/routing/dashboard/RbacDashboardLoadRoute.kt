/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.routing.dashboard

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
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUuidOrNull

/**
 * Retrieves the current session context and renders the RBAC dashboard based on the
 * user's permissions and role selections.
 * Redirects to the login screen if the session context is invalid.
 */
@RbacAPI
internal fun Route.rbacDashboardLoadRoute() {
    get("rbac/dashboard") {
        // Attempt to retrieve the session context for RBAC dashboard access. Redirect to the login screen if null.
        val sessionContext: SessionContext = RbacDashboardManager.getSessionContext(call = call)
            ?: return@get call.run {
                call.sessions.clear(name = SessionContext.SESSION_NAME)
                call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            }

        // Resolve the RBAC access details for the current session context.
        RbacDashboardManager.determineAccessDetails(
            sessionContext = sessionContext,
            roleId = call.parameters[RbacDashboardView.ROLE_KEY].toUuidOrNull()
        ).let { accessDetails ->
            // Respond with HTML view of the RBAC dashboard.
            call.respondHtml(status = HttpStatusCode.OK) {
                RbacDashboardView.build(
                    html = this,
                    isUpdated = false,
                    dashboardContext = accessDetails
                )
            }
        }
    }
}
