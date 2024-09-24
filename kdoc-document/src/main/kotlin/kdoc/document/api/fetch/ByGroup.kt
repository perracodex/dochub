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
import kdoc.core.env.SessionContext
import kdoc.core.env.SessionContext.Companion.getContext
import kdoc.core.persistence.utils.toUuid
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.model.Document
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.findDocumentsByGroupRoute() {
    /**
     * Find all documents by group.
     * @OpenAPITag Document - Find
     */
    get("v1/document/group/{group_id}") {
        val groupId: Uuid = call.parameters.getOrFail(name = "group_id").toUuid()
        val pageable: Pageable? = call.getPageable()

        val sessionContext: SessionContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by group", groupId = groupId, log = pageable?.toString())

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = service.findByGroupId(groupId = groupId, pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    }
}
