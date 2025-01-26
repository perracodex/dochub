/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.operate

import dochub.base.context.SessionContext
import dochub.base.context.sessionContext
import dochub.document.api.DocumentRouteApi
import dochub.document.model.Document
import dochub.document.service.DocumentAuditService
import dochub.document.service.DocumentService
import dochub.document.service.manager.DownloadManager
import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.perracodex.exposed.pagination.Page
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteApi
internal fun Route.backupDocumentsRoute() {
    get("/v1/document/backup") {
        // Audit the attempt operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "backup")

        // Get all documents.
        val documentService: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<Document> = documentService.findAll()
        if (documents.content.isEmpty()) {
            call.respond(status = HttpStatusCode.NoContent, message = "No documents found.")
            return@get
        }

        // Stream the backup to the client.
        val streamHandler: DownloadManager.StreamHandler = DownloadManager.prepareStream(
            documents = documents.content,
            decipher = false,
            archiveFilename = "backup",
            archiveAlways = true
        )
        DownloadManager.backupCountMetric.increment()
        call.response.header(HttpHeaders.ContentDisposition, streamHandler.contentDisposition.toString())
        call.respondOutputStream(contentType = streamHandler.contentType) {
            streamHandler.stream(this)
        }
    } api {
        tags = setOf("Document")
        summary = "Backup all documents."
        description = "Download a backup file containing all document entries."
        operationId = "backupDocuments"
        response(status = HttpStatusCode.NoContent) {
            description = "No documents found."
        }
    }
}
