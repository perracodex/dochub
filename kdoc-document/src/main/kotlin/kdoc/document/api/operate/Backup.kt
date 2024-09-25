/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.perracodex.exposed.pagination.Page
import kdoc.core.context.SessionContext
import kdoc.core.context.getContext
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.model.Document
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import kdoc.document.service.managers.DownloadManager
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.backupDocumentsRoute() {
    /**
     * Downloads a backup file containing all the documents.
     * @OpenAPITag Document - Operate
     */
    get("v1/document/backup") {
        // Audit the backup action.
        val sessionContext: SessionContext = call.getContext()
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
    }
}
