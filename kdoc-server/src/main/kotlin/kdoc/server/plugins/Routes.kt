/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.access.rbac.api.rbacRoutes
import kdoc.access.token.api.accessTokenRoutes
import kdoc.core.context.getContextOrNull
import kdoc.core.env.health.healthCheckRoute
import kdoc.core.events.sseRoute
import kdoc.core.security.snowflake.snowflakeRoute
import kdoc.document.api.documentRoutes

/**
 * Initializes and sets up routing for the application.
 *
 * Routing is the core Ktor plugin for handling incoming requests in a server application.
 * When the client makes a request to a specific URL (for example, /hello), the routing
 * mechanism allows us to define how we want this request to be served.
 *
 * #### References
 * - [Ktor Routing Documentation](https://ktor.io/docs/server-routing.html)
 * - [Application Structure](https://ktor.io/docs/server-application-structure.html) for examples
 * of how to organize routes in diverse ways.
 * - [Ktor Rate Limit](https://ktor.io/docs/server-rate-limit.html)
 */
internal fun Application.configureRoutes() {

    routing {
        documentRoutes()
        accessTokenRoutes()
        healthCheckRoute()
        snowflakeRoute()
        rbacRoutes()
        sseRoute()

        // Server root endpoint.
        get("/") {
            call.getContextOrNull()?.let {
                call.respondText(text = "Hello World. Welcome ${it.username}!")
            } ?: call.respondText(text = "Hello World.")
        }
    }
}
