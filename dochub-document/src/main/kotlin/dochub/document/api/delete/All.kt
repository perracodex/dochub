/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.delete

import dochub.base.context.SessionContext
import dochub.base.context.sessionContext
import dochub.document.api.DocumentRouteApi
import dochub.document.service.DocumentAuditService
import dochub.document.service.DocumentService
import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteApi
internal fun Route.deleteAllDocumentsRoute() {
    delete("/v1/document/") {
        // Audit the attempt operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "delete all")

        // Delete all documents.
        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val deletedCount: Int = service.deleteAll()
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    } api {
        tags = setOf("Document")
        summary = "Delete all documents."
        description = "Delete all document entries."
        operationId = "deleteAllDocuments"
        response<Int>(status = HttpStatusCode.OK) {
            description = "The number of documents deleted."
        }
    }
}
