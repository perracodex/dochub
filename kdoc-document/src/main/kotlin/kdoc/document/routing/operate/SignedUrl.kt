/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUuidOrNull
import kdoc.base.security.utils.SecureUrl
import kdoc.base.settings.AppSettings
import kdoc.base.utils.NetworkUtils
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.getDocumentSignedUrlRoute() {
    // Generate the signed URL for a document download.
    get("v1/document/url/{document_id?}/{group_id?}") {
        val documentId: Uuid? = call.request.queryParameters["document_id"].toUuidOrNull()
        val groupId: Uuid? = call.request.queryParameters["group_id"]?.toUuidOrNull()
        (documentId ?: groupId) ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "Either document_id or group_id must be provided.")
            return@get
        }

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "generate signed URL", documentId = documentId)

        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"
        val secureUrl: String = SecureUrl.generate(
            basePath = basePath,
            data = "document_id=${documentId ?: ""}&group_id=${groupId ?: ""}",
        )

        call.respond(status = HttpStatusCode.OK, message = secureUrl)
    }
}
