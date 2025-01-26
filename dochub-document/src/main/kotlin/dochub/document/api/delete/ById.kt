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
internal fun Route.deleteDocumentByIdRoute() {
    delete("/v1/document/{document_id}/") {
        val documentId: Uuid = call.parameters.getOrFail(name = "document_id").toUuid()

        // Audit the attempt operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "delete by id", documentId = documentId)

        // Delete the document by ID.
        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val deletedCount: Int = service.delete(documentId = documentId)
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    } api {
        tags = setOf("Document")
        summary = "Delete a document by ID."
        description = "Delete a document entry by ID."
        operationId = "deleteDocumentById"
        pathParameter<Uuid>(name = "document_id") {
            description = "The document ID to delete."
        }
        response<Int>(status = HttpStatusCode.OK) {
            description = "The number of documents deleted."
        }
    }
}
