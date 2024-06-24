/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.serializers.SUUID
import kdoc.base.persistence.utils.toUUIDOrNull
import kdoc.base.security.utils.SecureIO
import kdoc.base.security.utils.SecureUrl
import kdoc.base.settings.AppSettings
import kdoc.base.utils.NetworkUtils
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
        val token: String = call.request.queryParameters["token"]!!
        val signature: String = call.request.queryParameters["signature"]!!

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "download", log = "token=$token | signature=$signature")

        // Verify the token and signature to get the document ID.

        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"
        val documentId: SUUID? = SecureUrl.verify(
            basePath = basePath,
            token = token,
            signature = signature
        ).toUUIDOrNull()

        if (documentId == null) {
            // Log the failed verification attempt.
            call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
                .audit(operation = "download verification failed", log = "token=$token | signature=$signature")

            call.respond(status = HttpStatusCode.Forbidden, message = "Invalid or expired token.")
            return@get
        }

        // Get the document file storage reference.

        val service: DocumentStorage = call.scope.get<DocumentStorage> { parametersOf(sessionContext) }
        val documentFile: DocumentFileEntity? = service.getDocumentFile(documentId = documentId)

        // Stream the document file to the client.

        documentFile?.let {
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

        } ?: call.respond(status = HttpStatusCode.NotFound, message = "File not found.")
    }
}
