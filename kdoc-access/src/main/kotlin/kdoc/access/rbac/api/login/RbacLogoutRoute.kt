/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.api.login

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.view.RbacLoginView
import kdoc.base.env.SessionContext

/**
 * Manages the session termination and redirection to the login page.
 * Clears the current session and redirects the actor to ensure a clean logout process.
 */
@RbacAPI
internal fun Route.rbacLogoutRoute() {
    /**
     * Clears the session and redirects to the login page.
     * @OpenAPITag RBAC
     */
    post("rbac/logout") {
        call.sessions.clear(name = SessionContext.SESSION_NAME)
        call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
    }
}