/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.plugins

import dochub.access.context.SessionContextFactory
import dochub.access.domain.rbac.annotation.RbacApi
import dochub.access.domain.rbac.view.RbacLoginView
import dochub.base.context.clearSessionContext
import dochub.base.context.sessionContext
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

/**
 * Refreshes the default actors, and configures the RBAC form login authentication.
 *
 * Demonstrates how to use form-based authentication, in which case
 * principal are not propagated across different requests, so we
 * must use sessions to store the actor information.
 *
 * #### References
 * - [Basic Authentication](https://ktor.io/docs/server-basic-auth.html)
 */
@OptIn(RbacApi::class)
public fun Application.configureRbac() {

    // Configure the RBAC form login authentication.
    authentication {
        form(name = RbacLoginView.RBAC_LOGIN_PATH) {
            userParamName = RbacLoginView.KEY_USERNAME
            passwordParamName = RbacLoginView.KEY_PASSWORD

            challenge {
                call.clearSessionContext()
                call.respondRedirect(url = RbacLoginView.RBAC_LOGIN_PATH)
            }

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
