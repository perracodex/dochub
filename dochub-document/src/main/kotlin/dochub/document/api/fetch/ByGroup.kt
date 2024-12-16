/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.fetch

import dochub.base.context.SessionContext
import dochub.base.context.getContext
import dochub.base.util.toUuid
import dochub.document.api.DocumentRouteApi
import dochub.document.model.Document
import dochub.document.service.DocumentAuditService
import dochub.document.service.DocumentService
import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.pathParameter
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.getPageable
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteApi
internal fun Route.findDocumentsByGroupRoute() {
    get("/v1/document/group/{group_id}") {
        val groupId: Uuid = call.parameters.getOrFail(name = "group_id").toUuid()
        val pageable: Pageable? = call.getPageable()

        val sessionContext: SessionContext = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by group", groupId = groupId, log = pageable?.toString())

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = service.findByGroupId(groupId = groupId, pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    } api {
        tags = setOf("Document")
        summary = "Find documents by group."
        description = "Find all document entries by group."
        operationId = "findDocumentsByGroup"
        pathParameter<Uuid>(name = "group_id") {
            description = "The group ID to find documents for."
        }
        response<Page<Document>>(status = HttpStatusCode.OK) {
            description = "The list of documents."
        }
    }
}
