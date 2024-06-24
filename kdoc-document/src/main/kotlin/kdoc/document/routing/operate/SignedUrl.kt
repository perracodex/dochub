/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUUID
import kdoc.base.security.utils.SecureUrl
import kdoc.base.settings.AppSettings
import kdoc.base.utils.NetworkUtils
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import java.util.*

@DocumentRouteAPI
internal fun Route.getDocumentSignedUrl() {
    // Generate the signed URL for a document download.
    get("{document_id}/url") {
        val documentId: UUID = call.parameters["document_id"].toUUID()

        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "generate signed URL", documentId = documentId)

        val basePath = "${NetworkUtils.getServerUrl()}/${AppSettings.storage.downloadsBasePath}"
        val secureUrl: String = SecureUrl.generate(
            basePath = basePath,
            data = documentId.toString()
        )

        call.respond(status = HttpStatusCode.OK, message = secureUrl)
    }
}
