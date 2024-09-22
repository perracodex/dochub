/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.fetch

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.getPageable
import kdoc.base.env.CallContext
import kdoc.base.env.CallContext.Companion.getContext
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.model.Document
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.findAllDocumentsRoute() {
    /**
     * Find all existing documents.
     * @OpenAPITag Document - Find
     */
    get("v1/document/") {
        val pageable: Pageable? = call.getPageable()

        val callContext: CallContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(callContext) }
            .audit(operation = "find all", log = pageable?.toString())

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(callContext) }
        val documents: Page<Document> = service.findAll(pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    }
}
