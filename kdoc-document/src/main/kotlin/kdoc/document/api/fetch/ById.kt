/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.fetch

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kdoc.base.env.CallContext
import kdoc.base.env.CallContext.Companion.getContext
import kdoc.base.persistence.utils.toUuid
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.errors.DocumentError
import kdoc.document.model.Document
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.findDocumentByIdRoute() {
    /**
     * Find a document by ID.
     * @OpenAPITag Document - Find
     */
    get("v1/document/{document_id}/") {
        val documentId: Uuid = call.parameters.getOrFail(name = "document_id").toUuid()

        val callContext: CallContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(callContext) }
            .audit(operation = "find by document id", documentId = documentId)

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(callContext) }
        val document: Document = service.findById(documentId = documentId)
            ?: throw DocumentError.DocumentNotFound(documentId = documentId)

        call.respond(status = HttpStatusCode.OK, message = document)
    }
}
