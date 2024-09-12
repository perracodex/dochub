/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.actor.service

import kdoc.access.actor.model.ActorDto
import kdoc.access.actor.model.ActorRequest
import kdoc.access.actor.repository.IActorRepository
import kdoc.access.credential.CredentialService
import kdoc.access.rbac.repository.role.IRbacRoleRepository
import kdoc.access.rbac.service.RbacService
import kdoc.base.env.Tracer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.Uuid

/**
 * Service to handle Actor operations.
 *
 * @property actorRepository The [IActorRepository] to handle Actor persistence operations.
 * @property roleRepository The [IRbacRoleRepository] to handle RBAC role operations.
 */
internal class ActorService(
    private val actorRepository: IActorRepository,
    private val roleRepository: IRbacRoleRepository
) : KoinComponent {
    private val tracer = Tracer<ActorService>()

    /**
     * Finds the [ActorDto] for the given username.
     * @param username The username of the [ActorDto] to find.
     * @return The [ActorDto] for the given username, or null if it doesn't exist.
     */
    suspend fun findByUsername(username: String): ActorDto? = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findByUsername(username = username)
    }

    /**
     * Finds the [ActorDto] for the given id.
     *
     * @param actorId The id of the [ActorDto] to find.
     * @return The [ActorDto] for the given id, or null if it doesn't exist.
     */
    suspend fun findById(actorId: Uuid): ActorDto? = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findById(actorId = actorId)
    }

    /**
     * Finds all existing [ActorDto] entries.
     * @return A list with all existing [ActorDto] entries.
     */
    suspend fun findAll(): List<ActorDto> = withContext(Dispatchers.IO) {
        return@withContext actorRepository.findAll()
    }

    /**
     * Creates a new [ActorDto].
     * @param actorRequest The [ActorRequest] to create.
     * @return The id of the [ActorDto] created.
     */
    suspend fun create(actorRequest: ActorRequest): Uuid = withContext(Dispatchers.IO) {
        tracer.debug("Creating actor with username: ${actorRequest.username}")
        val actorId: Uuid = actorRepository.create(actorRequest = actorRequest)
        refresh(actorId = actorId)
        return@withContext actorId
    }

    /**
     * Updates an existing [ActorDto].
     *
     * @param actorId The id of the Actor to update.
     * @param actorRequest The new details for the [ActorDto].
     * @return How many records were updated.
     */
    suspend fun update(actorId: Uuid, actorRequest: ActorRequest): Int = withContext(Dispatchers.IO) {
        tracer.debug("Updating actor with ID: $actorId")
        val count: Int = actorRepository.update(actorId = actorId, actorRequest = actorRequest)
        refresh(actorId = actorId)
        return@withContext count
    }

    /**
     * Sets the lock status for the given [ActorDto].
     *
     * @param actorId The id of the [ActorDto] to lock/unlock.
     * @param isLocked Whether the [ActorDto] should be locked or unlocked.
     */
    @Suppress("unused")
    suspend fun setLockedState(actorId: Uuid, isLocked: Boolean): Unit = withContext(Dispatchers.IO) {
        tracer.debug("Setting lock state for actor with ID: $actorId")
        actorRepository.setLockedState(actorId = actorId, isLocked = isLocked)
        refresh(actorId = actorId)
    }

    /**
     * Checks if there are any Actors in the database, or if the given usernames exist.
     *
     * @param usernames The actors usernames to check for. If null, checks for any Actors.
     * @return True if there are actors for the given usernames in the database, false otherwise.
     */
    suspend fun actorsExist(usernames: List<String>? = null): Boolean = withContext(Dispatchers.IO) {
        return@withContext actorRepository.actorsExist(usernames = usernames)
    }

    /**
     * Refreshes the [CredentialService] and [RbacService] to reflect an Actor creation or update.
     *
     * @param actorId The id of the Actor to refresh.
     */
    private suspend fun refresh(actorId: Uuid) {
        tracer.info("Triggering refresh in Credentials and RBAC services for actor with ID: $actorId")

        val credentialService: CredentialService by inject()
        credentialService.refreshActor(actorId = actorId)

        val rbacService: RbacService by inject()
        rbacService.refreshActor(actorId = actorId)
    }
}
