/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.operate

import io.github.perracodex.kopapi.dsl.operation.api
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.core.context.getContext
import kdoc.core.persistence.util.toUuidOrNull
import kdoc.core.security.util.SecureUrl
import kdoc.core.settings.AppSettings
import kdoc.core.util.NetworkUtils
import kdoc.document.api.DocumentRouteApi
import kdoc.document.service.DocumentAuditService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteApi
internal fun Route.getDocumentSignedUrlRoute() {
    get("v1/document/url/{document_id?}/{group_id?}") {
        val documentId: Uuid? = call.request.queryParameters["document_id"].toUuidOrNull()
        val groupId: Uuid? = call.request.queryParameters["group_id"]?.toUuidOrNull()
        (documentId ?: groupId) ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "Either document_id or group_id must be provided.")
            return@get
        }

        call.scope.get<DocumentAuditService> { parametersOf(call.getContext()) }
            .audit(operation = "generate signed URL", documentId = documentId)

        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"
        val secureUrl: String = SecureUrl.generate(
            basePath = basePath,
            data = "document_id=${documentId ?: ""}&group_id=${groupId ?: ""}",
        )

        call.respond(status = HttpStatusCode.OK, message = secureUrl)
    } api {
        tags = setOf("Document")
        summary = "Generate a signed URL for a document download."
        description = "Generate a signed URL for a document download to provide temporary access to the document."
        operationId = "getDocumentSignedUrl"
        response(status = HttpStatusCode.OK) {
            description = "The signed URL for the document download."
        }
        response(status = HttpStatusCode.BadRequest) {
            description = "Either document_id or group_id must be provided."
        }
    }
}
