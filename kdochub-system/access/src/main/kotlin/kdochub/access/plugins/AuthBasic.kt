/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.access.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import kdochub.access.context.SessionContextFactory
import kdochub.base.context.clearContext
import kdochub.base.context.setContext
import kdochub.base.settings.AppSettings

/**
 * Configures the Basic authentication.
 *
 * The Basic authentication scheme is a part of the HTTP framework used for access control and authentication.
 * In this scheme, actor credentials are transmitted as username/password pairs encoded using Base64.
 *
 * #### References
 * - [Basic Authentication](https://ktor.io/docs/server-basic-auth.html)
 */
public fun Application.configureBasicAuthentication() {

    authentication {
        basic(name = AppSettings.security.basicAuth.providerName) {
            realm = AppSettings.security.basicAuth.realm

            validate { credential ->
                SessionContextFactory.from(credential = credential)?.let { sessionContext ->
                    return@validate this.setContext(sessionContext = sessionContext)
                }

                this.clearContext()
                return@validate null
            }
        }
    }
}
