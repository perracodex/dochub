/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.di

import kdoc.access.actor.repository.IActorRepository
import kdoc.access.rbac.repository.field_rule.IRbacFieldRuleRepository
import kdoc.access.rbac.repository.field_rule.RbacFieldRuleRepository
import kdoc.access.rbac.repository.role.IRbacRoleRepository
import kdoc.access.rbac.repository.role.RbacRoleRepository
import kdoc.access.rbac.repository.scope_rule.IRbacScopeRuleRepository
import kdoc.access.rbac.repository.scope_rule.RbacScopeRuleRepository
import kdoc.access.rbac.service.RbacService
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * RBAC dependency injection module.
 */
object RbacDomainInjection {

    /**
     * Get the dependency injection module for the RBAC domain.
     */
    fun get(): Module {
        return module {
            single<IRbacFieldRuleRepository> {
                RbacFieldRuleRepository()
            }

            single<IRbacScopeRuleRepository> {
                RbacScopeRuleRepository(fieldRuleRepository = get<IRbacFieldRuleRepository>())
            }

            single<IRbacRoleRepository> {
                RbacRoleRepository(scopeRuleRepository = get<IRbacScopeRuleRepository>())
            }

            single<RbacService> {
                RbacService(
                    actorRepository = get<IActorRepository>(),
                    roleRepository = get<IRbacRoleRepository>(),
                    scopeRuleRepository = get<IRbacScopeRuleRepository>()
                )
            }
        }
    }
}
