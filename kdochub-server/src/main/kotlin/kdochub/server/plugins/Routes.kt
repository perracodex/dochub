/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.server.plugins

import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdochub.access.domain.rbac.api.rbacRoutes
import kdochub.access.domain.token.api.accessTokenRoutes
import kdochub.base.context.getContextOrNull
import kdochub.base.security.snowflake.snowflakeRoute
import kdochub.document.api.documentRoutes
import kdochub.server.health.healthCheckRoute

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

        // Server root endpoint.
        get("/") {
            call.getContextOrNull()?.let {
                call.respondText(text = "Hello World. Welcome ${it.username}!")
            } ?: call.respondText(text = "Hello World.")
        } api {
            tags = setOf("Root")
            summary = "Root endpoint."
            description = "The root endpoint of the server."
            operationId = "root"
            response<String>(status = HttpStatusCode.OK) {
                description = "Root endpoint response."
            }
        }
    }
}
