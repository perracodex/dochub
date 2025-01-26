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
import io.github.perracodex.kopapi.dsl.parameter.queryParameter
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import java.io.OutputStream

@DocumentRouteApi
internal fun Route.downloadDocumentRoute() {
    get("/v1/document/download") {
        // Get the token and signature from the query parameters.
        val token: String = call.request.queryParameters.getOrFail(name = "token")
        val signature: String = call.request.queryParameters.getOrFail(name = "signature")

        // Audit the download attempt.
        val sessionContext: SessionContext = call.sessionContext
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
    } api {
        tags = setOf("Document")
        summary = "Download a document."
        description = "Download a document file using a token and signature."
        operationId = "downloadDocument"
        queryParameter<String>(name = "token") {
            description = "The download token."
        }
        queryParameter<String>(name = "signature") {
            description = "The download signature."
        }
        response(status = HttpStatusCode.BadRequest) {
            description = "Missing token or signature."
        }
        response(status = HttpStatusCode.Forbidden) {
            description = "Unable to initiate download."
        }
        response<OutputStream>(status = HttpStatusCode.OK) {
            description = "The document file stream."
        }
    }
}
