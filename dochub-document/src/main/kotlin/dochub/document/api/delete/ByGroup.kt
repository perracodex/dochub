/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.delete

import dochub.base.context.SessionContext
import dochub.base.context.sessionContext
import dochub.base.util.toUuid
import dochub.document.api.DocumentRouteApi
import dochub.document.service.DocumentAuditService
import dochub.document.service.DocumentService
import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.pathParameter
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteApi
internal fun Route.deleteDocumentsByGroupRoute() {
    delete("/v1/document/group/{group_id}") {
        val groupId: Uuid = call.parameters.getOrFail(name = "group_id").toUuid()

        // Audit the delete by group operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "delete by group", groupId = groupId)

        // Delete documents by group.
        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val deletedCount: Int = service.deleteByGroup(groupId = groupId)
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    } api {
        tags = setOf("Document")
        summary = "Delete documents by group."
        description = "Delete all document entries by group."
        operationId = "deleteDocumentsByGroup"
        pathParameter<Uuid>(name = "group_id") {
            description = "The group ID to delete documents for."
        }
        response<Int>(status = HttpStatusCode.OK) {
            description = "The number of documents deleted."
        }
    }
}
