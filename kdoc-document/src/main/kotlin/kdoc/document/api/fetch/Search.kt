/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.fetch

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.CallContext
import kdoc.base.env.CallContext.Companion.getContext
import kdoc.base.persistence.pagination.Page
import kdoc.base.persistence.pagination.Pageable
import kdoc.base.persistence.pagination.getPageable
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

        val callContext: CallContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(callContext) }
            .audit(operation = "search", log = "$request | ${pageable?.toString()}")

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(callContext) }
        val documents: Page<Document> = service.search(filterSet = request, pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    }
}
