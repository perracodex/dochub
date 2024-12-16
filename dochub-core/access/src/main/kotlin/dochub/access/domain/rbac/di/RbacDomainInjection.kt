/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.access.domain.rbac.di

import dochub.access.domain.rbac.repository.field.IRbacFieldRuleRepository
import dochub.access.domain.rbac.repository.field.RbacFieldRuleRepository
import dochub.access.domain.rbac.repository.role.IRbacRoleRepository
import dochub.access.domain.rbac.repository.role.RbacRoleRepository
import dochub.access.domain.rbac.repository.scope.IRbacScopeRuleRepository
import dochub.access.domain.rbac.repository.scope.RbacScopeRuleRepository
import dochub.access.domain.rbac.service.RbacService
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * RBAC dependency injection module.
 */
public object RbacDomainInjection {

    /**
     * Get the dependency injection module for the RBAC domain.
     */
    public fun get(): Module {
        return module {
            singleOf(::RbacFieldRuleRepository) {
                bind<IRbacFieldRuleRepository>()
            }

            singleOf(::RbacScopeRuleRepository) {
                bind<IRbacScopeRuleRepository>()
            }

            singleOf(::RbacRoleRepository) {
                bind<IRbacRoleRepository>()
            }

            singleOf(::RbacService)
        }
    }
}
