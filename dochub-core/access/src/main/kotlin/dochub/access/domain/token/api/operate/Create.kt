/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.domain.token.api.operate

import dochub.access.domain.token.annotation.TokenApi
import dochub.access.domain.token.service.TokenService
import dochub.base.context.sessionContext
import dochub.base.plugins.RateLimitScope
import dochub.base.settings.AppSettings
import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Generates a new JWT token using Basic Authentication.
 * This endpoint is rate-limited to prevent abuse and requires valid Basic Authentication credentials.
 *
 * #### References
 * - [Ktor JWT Authentication](https://ktor.io/docs/server-jwt.html)
 * - [Basic Authentication](https://ktor.io/docs/server-basic-auth.html)
 */
@TokenApi
internal fun Route.createTokenRoute() {
    rateLimit(configuration = RateLimitName(name = RateLimitScope.NEW_AUTH_TOKEN.key)) {
        authenticate(AppSettings.security.basicAuth.providerName, optional = !AppSettings.security.isEnabled) {
            post("/auth/token/create") {
                TokenService.createToken(sessionContext = call.sessionContext).let { response ->
                    call.respondText(
                        text = response.message,
                        status = response.statusCode,
                        contentType = ContentType.Text.Plain
                    )
                }
            } api {
                tags = setOf("Token")
                summary = "Create a new JWT token."
                description = "Generates a new JWT token using Basic Authentication."
                operationId = "createToken"
                basicSecurity(name = "TokenCreation") {
                    description = "Generates a new JWT token using Basic Authentication."
                }
                response<String>(status = HttpStatusCode.OK) {
                    description = "The generated JWT token."
                    contentType = setOf(ContentType.Text.Plain)
                }
                response(status = HttpStatusCode.Unauthorized) {
                    description = "No valid credentials provided."
                }
                response(status = HttpStatusCode.InternalServerError) {
                    description = "Failed to generate token."
                }
            }
        }
    }
}
