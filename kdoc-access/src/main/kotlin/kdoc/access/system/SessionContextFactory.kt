/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.system

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kdoc.access.actor.entity.ActorEntity
import kdoc.access.actor.service.ActorService
import kdoc.access.credential.CredentialService
import kdoc.base.env.SessionContext
import kdoc.base.env.Tracer
import kdoc.base.settings.AppSettings
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Factory class for creating [SessionContext] instances.
 */
internal object SessionContextFactory : KoinComponent {
    private val tracer = Tracer<SessionContextFactory>()

    /**
     * Creates a [SessionContext] instance from a JWT [JWTCredential].
     *
     * @param jwtCredential The [JWTCredential] containing actor-related claims.
     * @return A [SessionContext] instance if both actorId and role are present and valid, null otherwise.
     */
    fun from(jwtCredential: JWTCredential): SessionContext? {
        // Check if the JWT audience claim matches the configured audience.
        // This ensures the token is intended for the application.
        if (!jwtCredential.payload.audience.contains(AppSettings.security.jwtAuth.audience)) {
            tracer.error("Invalid JWT audience: ${jwtCredential.payload.audience}")
            return null
        }

        // Check if the JWT issuer matches the configured issuer.
        // This ensures the token was issued by a trusted source.
        if (jwtCredential.payload.issuer != AppSettings.security.jwtAuth.issuer) {
            tracer.error("Invalid JWT issuer: ${jwtCredential.payload.issuer}")
            return null
        }

        val payload: String? = jwtCredential.payload.getClaim(SessionContext.CLAIM_KEY)?.asString()

        if (payload.isNullOrBlank()) {
            tracer.error("Missing JWT payload.")
            return null
        }

        return payload.let {
            Json.decodeFromString<SessionContext>(string = it).run {
                SessionContext(
                    actorId = actorId,
                    username = username,
                    roleId = roleId,
                    schema = schema
                )
            }
        }
    }

    /**
     * Retrieves a [SessionContext] instance from the database given a [UserPasswordCredential].
     *
     * @param credential The [UserPasswordCredential] of the Actor to retrieve.
     * @return A [SessionContext] instance if the Actor exists, null otherwise.
     */
    suspend fun from(credential: UserPasswordCredential): SessionContext? {
        val credentialService: CredentialService by inject()
        val userIdPrincipal: UserIdPrincipal? = credentialService.authenticate(credential = credential)

        if (userIdPrincipal == null) {
            tracer.error("Invalid credentials.")
            return null
        }

        val username: String = userIdPrincipal.name
        val actorService: ActorService by inject()
        val actor: ActorEntity? = actorService.findByUsername(username = username)

        if (actor == null) {
            tracer.error("No actor found for username: $username")
            return null
        }

        return actor.let { actorDetails ->
            SessionContext(
                actorId = actorDetails.id,
                username = actorDetails.username,
                roleId = actorDetails.role.id
            )
        }
    }
}
