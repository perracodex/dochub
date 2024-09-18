/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.delete

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.CallContext
import kdoc.base.env.CallContext.Companion.getContext
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.deleteAllDocumentsRoute() {
    /**
     * Delete all document entries.
     * @OpenAPITag Document - Delete
     */
    delete("v1/document/") {
        val callContext: CallContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(callContext) }
            .audit(operation = "delete all")

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(callContext) }
        val deletedCount: Int = service.deleteAll()
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    }
}
