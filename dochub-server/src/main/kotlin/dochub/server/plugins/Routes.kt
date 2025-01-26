/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.server.plugins

import dochub.access.domain.rbac.api.rbacRoutes
import dochub.access.domain.token.api.accessTokenRoutes
import dochub.base.context.sessionContextOrNull
import dochub.base.security.snowflake.snowflakeRoute
import dochub.document.api.documentRoutes
import dochub.server.health.healthCheckRoute
import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            val greeting: String = call.sessionContextOrNull?.let { sessionContext ->
                "Hello World. Welcome ${sessionContext.username}!"
            } ?: "Hello World."
            call.respondText(text = greeting)
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
