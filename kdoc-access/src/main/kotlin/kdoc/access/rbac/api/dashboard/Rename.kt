/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.api.dashboard

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.util.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.service.RbacDashboardManager
import kdoc.access.rbac.view.RbacDashboardView
import kdoc.access.rbac.view.RbacLoginView
import kdoc.core.env.SessionContext
import kdoc.core.persistence.utils.toUuid
import kotlin.uuid.Uuid

/**
 * Processes updates to RBAC settings based on actor submissions from the dashboard form.
 * Validates [SessionContext] and authorizes modifications, redirecting to the login screen if unauthorized.
 */
@RbacAPI
internal fun Route.rbacDashboardUpdateRoute() {
    /**
     * Processes updates to RBAC settings based on actor submissions from the dashboard form.
     * @OpenAPITag RBAC
     */
    post("rbac/dashboard") {
        // Retrieve the SessionContext or redirect to the login screen if it's missing.
        val sessionContext: SessionContext = RbacDashboardManager.getSessionContext(call = call)
            ?: return@post call.run {
                call.sessions.clear(name = SessionContext.SESSION_NAME)
                call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            }

        // Receive and process form parameters.
        val parameters: Parameters = call.receiveParameters()
        val currentRoleId: Uuid = parameters.getOrFail(name = RbacDashboardView.ROLE_KEY).toUuid()

        // Fetch the role-specific scope rules for the current role,
        // and update the rules based on the submitted parameters.
        RbacDashboardManager.processUpdate(
            sessionContext = sessionContext,
            roleId = currentRoleId,
            updates = parameters.entries().associate { it.key to it.value.first() }
        ).let { result ->
            when (result) {
                // If the update was successful, render the updated RBAC dashboard.
                is RbacDashboardManager.UpdateResult.Success -> call.respondHtml(HttpStatusCode.OK) {
                    RbacDashboardView.build(
                        html = this,
                        isUpdated = true,
                        dashboardContext = result.dashboardContext
                    )
                }

                // If the update was unauthorized, clear the session and redirect to the login screen.
                is RbacDashboardManager.UpdateResult.Unauthorized -> call.run {
                    sessions.clear(name = SessionContext.SESSION_NAME)
                    respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
                }
            }
        }
    }
}
