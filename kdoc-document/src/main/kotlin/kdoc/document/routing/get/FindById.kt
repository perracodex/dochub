/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.get

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUUID
import kdoc.document.entity.DocumentEntity
import kdoc.document.errors.DocumentError
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import java.util.*

@DocumentRouteAPI
internal fun Route.findDocumentById() {
    // Find a documents by ID.
    get {
        val documentId: UUID = call.parameters["document_id"].toUUID()

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by document id", documentId = documentId)

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val document: DocumentEntity? = service.findById(documentId = documentId)

        document?.let {
            call.respond(status = HttpStatusCode.OK, message = document)
        } ?: DocumentError.DocumentNotFound(documentId = documentId).raise()
    }
}
