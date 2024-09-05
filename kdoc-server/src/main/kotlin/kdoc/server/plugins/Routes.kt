/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.access.rbac.routing.rbacRoute
import kdoc.access.token.accessTokenRoute
import kdoc.base.env.SessionContext
import kdoc.base.env.health.routing.healthCheckRoute
import kdoc.base.events.sseRoute
import kdoc.base.security.snowflake.snowflakeRoute
import kdoc.document.routing.documentRoute

/**
 * Initializes and sets up routing for the application.
 *
 * Routing is the core Ktor plugin for handling incoming requests in a server application.
 * When the client makes a request to a specific URL (for example, /hello), the routing
 * mechanism allows us to define how we want this request to be served.
 *
 * See: [Ktor Routing Documentation](https://ktor.io/docs/server-routing.html)
 *
 * See [Application Structure](https://ktor.io/docs/server-application-structure.html) for examples
 * of how to organize routes in diverse ways.
 *
 * See: [Ktor Rate Limit](https://ktor.io/docs/server-rate-limit.html)
 */
internal fun Application.configureRoutes() {

    routing {
        documentRoute()
        accessTokenRoute()
        healthCheckRoute()
        snowflakeRoute()
        rbacRoute()
        sseRoute()

        // Server root endpoint.
        get("/") {
            val sessionContext: SessionContext? = SessionContext.from(call = call)
            sessionContext?.let {
                call.respondText(text = "Hello World. Welcome ${it.username}!")
            } ?: call.respondText(text = "Hello World.")
        }
    }
}
