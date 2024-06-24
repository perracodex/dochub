/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.server.plugins

import io.ktor.server.application.*
import kdoc.access.actor.di.ActorDomainInjection
import kdoc.access.rbac.di.RbacDomainInjection
import kdoc.document.di.DocumentDomainInjection
import org.koin.ktor.plugin.Koin

/**
 * Sets up and initializes dependency injection using the Koin framework.
 *
 * See: [Koin for Ktor Documentation](https://insert-koin.io/docs/quickstart/ktor)
 */
fun Application.configureKoin() {

    install(plugin = Koin) {
        modules(
            RbacDomainInjection.get(),
            ActorDomainInjection.get(),
            DocumentDomainInjection.get(),
        )
    }
}
