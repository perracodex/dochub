/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.actor.di

import kdoc.access.actor.repository.ActorRepository
import kdoc.access.actor.repository.IActorRepository
import kdoc.access.actor.service.ActorService
import kdoc.access.credential.CredentialService
import kdoc.access.rbac.repository.role.IRbacRoleRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Actor dependency injection module.
 */
public object ActorDomainInjection {

    /**
     * Get the dependency injection module for the Actor domain.
     */
    public fun get(): Module {
        return module {
            single<IActorRepository>(createdAtStart = true) {
                ActorRepository(roleRepository = get<IRbacRoleRepository>())
            }

            single<CredentialService>(createdAtStart = true) {
                CredentialService()
            }

            single<ActorService>(createdAtStart = true) {
                ActorService(
                    roleRepository = get<IRbacRoleRepository>(),
                    actorRepository = get<IActorRepository>()
                )
            }
        }
    }
}
