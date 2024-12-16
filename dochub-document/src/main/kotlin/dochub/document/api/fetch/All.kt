/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.fetch

import dochub.base.context.SessionContext
import dochub.base.context.getContext
import dochub.document.api.DocumentRouteApi
import dochub.document.model.Document
import dochub.document.service.DocumentAuditService
import dochub.document.service.DocumentService
import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.queryParameter
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.perracodex.exposed.pagination.Page
import io.perracodex.exposed.pagination.Pageable
import io.perracodex.exposed.pagination.getPageable
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteApi
internal fun Route.findAllDocumentsRoute() {
    get("/v1/document/") {
        val pageable: Pageable? = call.getPageable()

        val sessionContext: SessionContext = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "find all", log = pageable?.toString())

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = service.findAll(pageable = pageable)
        call.respond(status = HttpStatusCode.OK, message = documents)
    } api {
        tags = setOf("Document")
        summary = "Find all documents."
        description = "Find all document entries."
        operationId = "findAllDocuments"
        queryParameter<String>(name = "page") {
            description = "The page number information."
            required = false
        }
        queryParameter<Int>(name = "size") {
            description = "The page size information."
            required = false
        }
        queryParameter<Pageable.PageSort>(name = "sort") {
            description = "The page sort information."
            required = false
        }
        response<Page<Document>>(status = HttpStatusCode.OK) {
            description = "The list of documents."
        }
    }
}
