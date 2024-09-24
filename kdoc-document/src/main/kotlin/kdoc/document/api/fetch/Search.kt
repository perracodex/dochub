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
import kdoc.base.env.SessionContext
import kdoc.base.env.SessionContext.Companion.getContext
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.model.Document
import kdoc.document.model.DocumentFilterSet
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.searchDocumentsRoute() {
    /**
     * Search (filter) for documents.
     * @OpenAPITag Document - Find
     */
    post<DocumentFilterSet>("v1/document/search") { request ->
        val pageable: Pageable? = call.getPageable()

        val sessionContext: SessionContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "search", log = "$request | ${pageable?.toString()}")

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = service.search(filterSet = request, pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    }
}
