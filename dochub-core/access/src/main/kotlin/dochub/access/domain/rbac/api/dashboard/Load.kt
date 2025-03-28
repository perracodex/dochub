/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.domain.rbac.api.dashboard

import dochub.access.domain.rbac.annotation.RbacApi
import dochub.access.domain.rbac.service.RbacDashboardManager
import dochub.access.domain.rbac.view.RbacDashboardView
import dochub.access.domain.rbac.view.RbacLoginView
import dochub.base.context.SessionContext
import dochub.base.context.clearSessionContext
import dochub.base.context.sessionContext
import dochub.base.util.toUuidOrNull
import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Retrieves the current [SessionContext] and renders the RBAC dashboard based
 * on the actor's permissions and role selections.
 * Redirects to the login screen if the [SessionContext] is invalid.
 */
@RbacApi
internal fun Route.rbacDashboardLoadRoute() {
    get("rbac/dashboard") {
        // Attempt to retrieve the SessionContext for RBAC dashboard access. Redirect to the login screen if null.
        val sessionContext: SessionContext = call.sessionContext
        if (!RbacDashboardManager.hasPermission(sessionContext = sessionContext)) {
            call.clearSessionContext()
            call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            return@get
        }

        // Resolve the RBAC access details for the current SessionContext.
        RbacDashboardManager.determineAccessDetails(
            sessionContext = sessionContext,
            roleId = call.parameters[RbacDashboardView.ROLE_KEY].toUuidOrNull()
        ).let { dashboardContext ->
            // Respond with HTML view of the RBAC dashboard.
            call.respondHtml(status = HttpStatusCode.OK) {
                RbacDashboardView.build(
                    html = this,
                    isUpdated = false,
                    dashboardContext = dashboardContext
                )
            }
        }
    } api {
        tags = setOf("RBAC")
        summary = "Load the RBAC dashboard."
        description = "Load the RBAC dashboard to view and manage role-based access control settings."
        operationId = "rbacDashboardLoad"
        response<String>(status = HttpStatusCode.OK) {
            description = "The RBAC dashboard."
        }
        response<String>(status = HttpStatusCode.Found) {
            description = "Redirect to the RBAC login page."
        }
    }
}
