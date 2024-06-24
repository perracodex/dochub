/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.security.utils.SecureIO
import kdoc.document.entity.DocumentFileEntity
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentStorage
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import java.io.FileInputStream

@DocumentRouteAPI
internal fun Route.downloadDocument() {
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

        // Get the document file storage reference.
        val service: DocumentStorage = call.scope.get<DocumentStorage> { parametersOf(sessionContext) }
        val documentFile: DocumentFileEntity = service.getSignedDocumentFile(token = token, signature = signature) ?: run {
            auditService.audit(operation = "download verification failed", log = "token=$token | signature=$signature")
            call.respond(status = HttpStatusCode.Forbidden, message = "Unable to initiate download.")
            return@get
        }

        // Stream the document file to the client.
        streamDocumentFile(call = call, documentFile = documentFile)
    }
}

private suspend fun streamDocumentFile(call: ApplicationCall, documentFile: DocumentFileEntity) {
    DocumentStorage.downloadCountMetric.increment()

    call.response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(
            ContentDisposition.Parameters.FileName,
            documentFile.originalName
        ).toString()
    )

    call.respondOutputStream(contentType = ContentType.Application.OctetStream) {
        FileInputStream(documentFile.file).use { inputStream ->
            if (documentFile.isCiphered) {
                SecureIO.decipher(input = inputStream, output = this)
            } else {
                inputStream.copyTo(out = this)
            }
        }
    }
}
