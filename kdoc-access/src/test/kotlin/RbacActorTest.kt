/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import kdoc.access.actor.di.ActorDomainInjection
import kdoc.access.actor.entity.ActorEntity
import kdoc.access.actor.entity.ActorRequest
import kdoc.access.actor.service.ActorService
import kdoc.access.rbac.di.RbacDomainInjection
import kdoc.access.rbac.entity.role.RbacRoleEntity
import kdoc.access.rbac.entity.role.RbacRoleRequest
import kdoc.access.rbac.entity.scope_rule.RbacScopeRuleRequest
import kdoc.access.rbac.service.RbacService
import kdoc.base.database.schema.admin.rbac.types.RbacAccessLevel
import kdoc.base.database.schema.admin.rbac.types.RbacScope
import kdoc.base.utils.TestUtils
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.test.*

/**
 * Test for the [RbacService].
 * Not for RBAC permission checks, but instead for the service interface.
 * For an example of RBAC permission checks, see the RBAC tests in the application service.
 */
class RbacActorTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        TestUtils.setupDatabase()
        TestUtils.setupKoin(modules = listOf(RbacDomainInjection.get(), ActorDomainInjection.get()))
    }

    @AfterTest
    fun tearDown() {
        TestUtils.tearDown()
    }

    @Test
    fun testActor(): Unit = testSuspend {
        val roleName = "any_role_name"
        val description = "Any role description"

        val roleRequest = RbacRoleRequest(
            roleName = roleName,
            description = description,
            isSuper = false,
            scopeRules = listOf(
                RbacScopeRuleRequest(
                    scope = RbacScope.SYSTEM_ADMIN,
                    accessLevel = RbacAccessLevel.FULL
                )
            )
        )

        val rbacService: RbacService by inject()

        // Create the role.
        val roleEntity: RbacRoleEntity = rbacService.createRole(roleRequest = roleRequest)

        // Create the actor.
        val actorService: ActorService by inject()

        val username = "any_username"
        val password = "any_password"
        val actorId: UUID = actorService.create(
            actorRequest = ActorRequest(
                roleId = roleEntity.id,
                username = username,
                password = password,
                isLocked = false
            )
        )

        var actor: ActorEntity? = actorService.findByUsername(username = username)
        assertNotNull(actual = actor, message = "The user was not found in the database after it was created.")
        assertEquals(expected = username, actual = actor.username)
        assertEquals(expected = password, actual = actor.password)

        actor = actorService.findById(actorId = actorId)
        assertNotNull(actual = actor, message = "The user was not found in the database after it was created.")
        assertEquals(expected = username, actual = actor.username)
        assertEquals(expected = password, actual = actor.password)

        // Try to create the same actor again.
        // This should fail because the username is unique.
        assertFailsWith<ExposedSQLException> {
            actorService.create(
                actorRequest = ActorRequest(
                    roleId = roleEntity.id,
                    username = username,
                    password = password,
                    isLocked = false
                )
            )
        }

        // Find a role by Actor ID.
        val roleByActor: RbacRoleEntity? = rbacService.findRoleByActorId(actorId = actorId)
        assertNotNull(actual = roleByActor, message = "The role was not found in the database after it was created.")
        assertEquals(expected = roleName, actual = roleByActor.roleName)
        assertEquals(expected = description, actual = roleByActor.description)
    }
}
