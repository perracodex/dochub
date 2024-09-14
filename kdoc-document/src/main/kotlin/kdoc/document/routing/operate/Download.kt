/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kdoc.base.env.SessionContext
import kdoc.document.model.Document
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import kdoc.document.service.managers.DownloadManager
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.downloadDocumentRoute() {
    // Serve a document file to download.
    get("v1/document/download/{token?}/{signature?}") {
        val token: String = call.request.queryParameters.getOrFail(name = "token")
        val signature: String = call.request.queryParameters.getOrFail(name = "signature")

        // Audit the download attempt.
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        val auditService: DocumentAuditService = call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
        auditService.audit(operation = "download", log = "token=$token | signature=$signature")

        // If the token or signature is missing, return a bad request response.
        if (token.isBlank() || signature.isBlank()) {
            call.respond(status = HttpStatusCode.BadRequest, message = "Missing token or signature.")
            return@get
        }

        // Get all document files for the given token and signature.
        val documentService: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: List<Document>? = documentService.findBySignature(token = token, signature = signature)
        if (documents.isNullOrEmpty()) {
            auditService.audit(operation = "download verification failed", log = "token=$token | signature=$signature")
            call.respond(status = HttpStatusCode.Forbidden, message = "Unable to initiate download.")
            return@get
        }

        // Stream the document file to the client.
        val streamHandler: DownloadManager.StreamHandler = DownloadManager.prepareStream(
            documents = documents,
            decipher = true,
            archiveFilename = "download",
            archiveAlways = false
        )
        DownloadManager.downloadCountMetric.increment()
        call.response.header(HttpHeaders.ContentDisposition, streamHandler.contentDisposition.toString())
        call.respondOutputStream(contentType = streamHandler.contentType) {
            streamHandler.stream(this)
        }
    }
}
