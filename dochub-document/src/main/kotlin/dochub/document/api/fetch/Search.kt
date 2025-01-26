/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.fetch

import dochub.base.context.SessionContext
import dochub.base.context.sessionContext
import dochub.document.api.DocumentRouteApi
import dochub.document.model.Document
import dochub.document.model.DocumentFilterSet
import dochub.document.service.DocumentAuditService
import dochub.document.service.DocumentService
import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.getPageable
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteApi
internal fun Route.searchDocumentsRoute() {
    post<DocumentFilterSet>("/v1/document/search") { request ->
        val pageable: Pageable? = call.getPageable()

        // Audit the attempt operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "search", log = "$request | ${pageable?.toString()}")

        // Search for documents.
        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = service.search(filterSet = request, pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    } api {
        tags = setOf("Document")
        summary = "Search for documents."
        description = "Search (filter) for document entries."
        operationId = "searchDocuments"
        requestBody<DocumentFilterSet> {
            description = "The filter set to search for documents."
        }
        response<Page<Document>>(status = HttpStatusCode.OK) {
            description = "The list of documents."
        }
    }
}
