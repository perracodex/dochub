/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.delete

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.deleteAllDocumentsRoute() {
    // Delete all documents.
    delete("v1/document/") {
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "delete all")

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val deletedCount: Int = service.deleteAll()
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    }
}
