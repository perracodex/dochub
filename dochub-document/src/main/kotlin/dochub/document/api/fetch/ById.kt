/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.fetch

import dochub.base.context.SessionContext
import dochub.base.context.sessionContext
import dochub.base.error.AppException
import dochub.base.util.toUuid
import dochub.document.api.DocumentRouteApi
import dochub.document.error.DocumentError
import dochub.document.model.Document
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
internal fun Route.findDocumentByIdRoute() {
    get("/v1/document/{document_id}/") {
        val documentId: Uuid = call.parameters.getOrFail(name = "document_id").toUuid()

        // Audit the attempt operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by document id", documentId = documentId)

        // Find the document by ID.
        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val document: Document = service.findById(documentId = documentId)
            ?: throw DocumentError.DocumentNotFound(documentId = documentId)

        call.respond(status = HttpStatusCode.OK, message = document)
    } api {
        tags = setOf("Document")
        summary = "Find a document by ID."
        description = "Find a document entry by ID."
        operationId = "findDocumentById"
        pathParameter<Uuid>(name = "document_id") {
            description = "The document ID to find."
        }
        response<Document>(status = HttpStatusCode.OK) {
            description = "The document."
        }
        response<AppException.ErrorResponse>(status = DocumentError.DocumentNotFound.STATUS_CODE) {
            description = "The document was not found. Code ${DocumentError.DocumentNotFound.ERROR_CODE}"
        }
    }
}
