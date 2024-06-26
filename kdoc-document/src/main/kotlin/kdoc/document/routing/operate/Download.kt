/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.document.entity.DocumentEntity
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import kdoc.document.service.DocumentStorageService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.downloadDocumentRoute() {
    // Serve a document file to download.
    get("download/{token?}/{signature?}") {
        val token: String? = call.request.queryParameters["token"]
        val signature: String? = call.request.queryParameters["signature"]

        // Audit the download attempt.
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        val auditService: DocumentAuditService = call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
        auditService.audit(operation = "download", log = "token=$token | signature=$signature")

        // If the token or signature is missing, return a bad request response.
        if (token.isNullOrBlank() || signature.isNullOrBlank()) {
            call.respond(status = HttpStatusCode.BadRequest, message = "Missing token or signature.")
            return@get
        }

        // Get all document files for the given token and signature.
        val documentService: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: List<DocumentEntity>? = documentService.findBySignature(token = token, signature = signature)
        if (documents.isNullOrEmpty()) {
            auditService.audit(operation = "download verification failed", log = "token=$token | signature=$signature")
            call.respond(status = HttpStatusCode.Forbidden, message = "Unable to initiate download.")
            return@get
        }

        // Stream the document file to the client.
        val storageService: DocumentStorageService = call.scope.get<DocumentStorageService> { parametersOf(sessionContext) }
        DocumentStorageService.downloadCountMetric.increment()
        if (documents.size == 1) {
            storageService.streamDocumentFile(call = call, document = documents.first(), decipher = true)
        } else {
            storageService.streamZip(call = call, filename = "download", documents = documents, decipher = true)
        }
    }
}
