/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.routing.dashboard

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.service.RbacDashboardManager
import kdoc.access.rbac.view.RbacDashboardView
import kdoc.access.rbac.view.RbacLoginView
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUuidOrNull
import kotlin.uuid.Uuid

/**
 * Handles the POST request for the RBAC dashboard, processing updates to RBAC settings based on user input.
 * This includes adjusting roles, scopes, and permissions according to form submissions. The function ensures
 * that only authorized modifications are applied, with redirects handling any unauthorized access attempts.
 */
@RbacAPI
internal fun Route.rbacDashboardUpdateRoute() {
    post("rbac/dashboard") {
        // Retrieve session context or redirect to the login screen if it's missing.
        val sessionContext: SessionContext = RbacDashboardManager.getSessionContext(call = call)
            ?: return@post call.run {
                call.sessions.clear(name = SessionContext.SESSION_NAME)
                call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            }

        // Receive and process form parameters.
        val parameters: Parameters = call.receiveParameters()
        val currentRoleId: Uuid = parameters[RbacDashboardView.ROLE_KEY].toUuidOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid or missing role ID.")

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
