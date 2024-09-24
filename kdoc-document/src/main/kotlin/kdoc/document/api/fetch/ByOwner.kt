/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.fetch

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.getPageable
import kdoc.base.env.SessionContext
import kdoc.base.env.SessionContext.Companion.getContext
import kdoc.base.persistence.utils.toUuid
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.model.Document
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.findDocumentsByOwnerRoute() {
    /**
     * Find all documents by owner.
     * @OpenAPITag Document - Find
     */
    get("v1/document/owner/{owner_id}") {
        val ownerId: Uuid = call.parameters.getOrFail(name = "owner_id").toUuid()
        val pageable: Pageable? = call.getPageable()

        val sessionContext: SessionContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by owner", ownerId = ownerId, log = pageable?.toString())

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = service.findByOwnerId(ownerId = ownerId, pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    }
}
