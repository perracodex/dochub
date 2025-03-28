/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.operate

import dochub.base.context.sessionContext
import dochub.base.security.util.SecureUrl
import dochub.base.settings.AppSettings
import dochub.base.util.NetworkUtils
import dochub.base.util.toUuidOrNull
import dochub.document.api.DocumentRouteApi
import dochub.document.service.DocumentAuditService
import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.queryParameter
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteApi
internal fun Route.getDocumentSignedUrlRoute() {
    get("/v1/document/url") {
        // Get the document ID or group ID from the query parameters.
        val documentId: Uuid? = call.request.queryParameters["document_id"].toUuidOrNull()
        val groupId: Uuid? = call.request.queryParameters["group_id"]?.toUuidOrNull()
        (documentId ?: groupId) ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "Either document_id or group_id must be provided.")
            return@get
        }

        // Audit the attempt operation.
        call.scope.get<DocumentAuditService> { parametersOf(call.sessionContext) }
            .audit(operation = "generate signed URL", documentId = documentId)

        // Generate the signed URL for the document download.
        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"
        val secureUrl: String = SecureUrl.generate(
            basePath = basePath,
            data = "document_id=${documentId ?: ""}&group_id=${groupId ?: ""}",
        )

        // Respond with the signed URL.
        call.respond(status = HttpStatusCode.OK, message = secureUrl)
    } api {
        tags = setOf("Document")
        summary = "Generate a signed URL for a document download."
        description = "Generate a signed URL for a document download to provide temporary access to the document."
        operationId = "getDocumentSignedUrl"
        queryParameter<Uuid>(name = "document_id") {
            description = "The document ID."
            required = false
        }
        queryParameter<Uuid>(name = "group_id") {
            description = "The group ID."
            required = false
        }
        response(status = HttpStatusCode.OK) {
            description = "The signed URL for the document download."
        }
        response(status = HttpStatusCode.BadRequest) {
            description = "Either document_id or group_id must be provided."
        }
    }
}
