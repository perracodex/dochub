/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.context

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kdoc.access.actor.model.Actor
import kdoc.access.actor.service.ActorService
import kdoc.access.credential.CredentialService
import kdoc.access.plugins.configureBasicAuthentication
import kdoc.access.plugins.configureJwtAuthentication
import kdoc.base.env.SessionContext
import kdoc.base.env.Tracer
import kdoc.base.settings.AppSettings
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Provides factory methods for constructing [SessionContext] instances from various authentication credential flows.
 * This object is primarily utilized by authentication mechanisms like JWT and OAuth to validate credentials
 * and generate [SessionContext] instances. These instances are crucial for populating the [ApplicationCall]
 * with session details and actor-specific information throughout the lifecycle of an API call.
 *
 * Using this factory ensures that all authentication methods adhere to a consistent approach in
 * constructing [SessionContext] instances, which is vital for security and traceability within the application.
 *
 * @see configureJwtAuthentication
 * @see configureBasicAuthentication
 */
internal object SessionContextFactory : KoinComponent {
    private val tracer = Tracer<SessionContextFactory>()

    /**
     * Creates a [SessionContext] instance from a JWT [JWTCredential].
     *
     * @param jwtCredential The [JWTCredential] containing actor-related claims.
     * @return A [SessionContext] instance if actor details and validations pass; null otherwise.
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

        // Extract the serialized SessionContext from the JWT claims.
        // This payload contains key session details serialized as a string,
        // intended for reconstructing the SessionContext.
        // If absent or blank, it indicates the JWT does not contain the required SessionContext data.
        val payload: String? = jwtCredential.payload.getClaim(SessionContext.CLAIM_KEY)?.asString()
        if (payload.isNullOrBlank()) {
            tracer.error("Missing JWT payload.")
            return null
        }

        // Return a fully constructed SessionContext for the reconstructed payload.
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
     * Creates a [SessionContext] by authenticating a [UserPasswordCredential].
     * Authenticates the actor's credentials and retrieves actor details from the database.
     *
     * @param credential The [UserPasswordCredential] of the actor attempting to authenticate.
     * @return A [SessionContext] instance if actor details and validations pass; null otherwise.
     */
    suspend fun from(credential: UserPasswordCredential): SessionContext? {
        // Resolve the UserIdPrincipal. Return null if the authentication fails to provide it.
        val credentialService: CredentialService by inject()
        val userIdPrincipal: UserIdPrincipal = credentialService.authenticate(credential = credential) ?: run {
            tracer.error("Failed to resolve UserIdPrincipal. Invalid credentials.")
            return null
        }

        // Resolve the actor. Return null if no actor corresponds to the provided username.
        val username: String = userIdPrincipal.name
        val actorService: ActorService by inject()
        val actor: Actor = actorService.findByUsername(username = username) ?: run {
            tracer.error("No actor found for username: $username")
            return null
        }

        // Return a fully constructed SessionContext for the authenticated actor.
        return actor.let { actorDetails ->
            SessionContext(
                actorId = actorDetails.id,
                username = actorDetails.username,
                roleId = actorDetails.role.id
            )
        }
    }
}
