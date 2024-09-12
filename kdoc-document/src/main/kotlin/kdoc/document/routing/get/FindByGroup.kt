/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.get

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.pagination.Page
import kdoc.base.persistence.pagination.Pageable
import kdoc.base.persistence.pagination.getPageable
import kdoc.base.persistence.utils.toUuid
import kdoc.document.entity.DocumentDto
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.findDocumentsByGroupRoute() {
    // Find all documents by group.
    get("v1/document/group/{group_id}") {
        val groupId: Uuid = call.parameters["group_id"].toUuid()
        val pageable: Pageable? = call.getPageable()

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by group", groupId = groupId, log = pageable?.toString())

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<DocumentDto> = service.findByGroupId(groupId = groupId, pageable = pageable)

        call.respond(status = HttpStatusCode.OK, message = documents)
    }
}
