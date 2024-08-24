/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.get

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUuid
import kdoc.document.entity.DocumentEntity
import kdoc.document.errors.DocumentError
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.findDocumentByIdRoute() {
    // Find a documents by ID.
    get {
        val documentId: Uuid = call.parameters["document_id"].toUuid()

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find by document id", documentId = documentId)

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val document: DocumentEntity? = service.findById(documentId = documentId)

        document?.let {
            call.respond(status = HttpStatusCode.OK, message = document)
        } ?: throw DocumentError.DocumentNotFound(documentId = documentId)
    }
}
