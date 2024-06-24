/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.routing.login

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kdoc.access.rbac.plugin.annotation.RbacAPI
import kdoc.access.rbac.view.RbacLoginView
import kdoc.base.env.SessionContext

/**
 * The route for logging out of the RBAC admin panel.
 */
@RbacAPI
fun Route.rbacLogoutRoute() {
    post("rbac/logout") {
        call.sessions.clear(name = SessionContext.SESSION_NAME)
        call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
    }
}
