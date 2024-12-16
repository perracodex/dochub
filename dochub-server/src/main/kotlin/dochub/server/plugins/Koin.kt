/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.server.plugins

import dochub.access.domain.actor.di.ActorDomainInjection
import dochub.access.domain.rbac.di.RbacDomainInjection
import dochub.document.di.DocumentDomainInjection
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

/**
 * Sets up and initializes dependency injection using the Koin framework.
 *
 * #### References
 * - [Koin for Ktor Documentation](https://insert-koin.io/docs/quickstart/ktor)
 */
internal fun Application.configureKoin() {

    install(plugin = Koin) {
        modules(
            // Load all the DI modules for the application.
            RbacDomainInjection.get(),
            ActorDomainInjection.get(),
            DocumentDomainInjection.get(),
        )
    }
}
