/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.delete

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUuid
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.deleteDocumentByIdRoute() {
    // Delete a document by ID.
    delete("v1/document/{document_id}/") {
        val documentId: Uuid = call.parameters["document_id"].toUuid()

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "delete by id", documentId = documentId)

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val deletedCount: Int = service.delete(documentId = documentId)

        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    }
}
