/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.plugins

import dochub.access.context.SessionContextFactory
import dochub.base.context.clearSessionContext
import dochub.base.context.sessionContext
import dochub.base.settings.AppSettings
import io.ktor.server.application.*
import io.ktor.server.auth.*

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
                    this.sessionContext = sessionContext
                    return@validate sessionContext
                }

                this.clearSessionContext()
                return@validate null
            }
        }
    }
}
