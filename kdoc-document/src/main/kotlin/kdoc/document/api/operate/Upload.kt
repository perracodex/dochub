/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.operate

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.SessionContext
import kdoc.base.persistence.utils.toUuid
import kdoc.base.persistence.utils.toUuidOrNull
import kdoc.base.settings.AppSettings
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.model.Document
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.managers.upload.UploadManager
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.uploadDocumentsRoute() {
    /**
     * Upload a new document.
     * @OpenAPITag Document - Operate
     */
    post("v1/document/{owner_id?}/{group_id?}/{type?}/{cipher?}") {
        val ownerId: Uuid = call.request.queryParameters.getOrFail(name = "owner_id").toUuid()
        val groupId: Uuid? = call.request.queryParameters["group_id"].toUuidOrNull()
        val type: DocumentType = DocumentType.parse(value = call.request.queryParameters.getOrFail(name = "type"))
        val cipher: Boolean = call.request.queryParameters["cipher"]?.toBoolean()
            ?: AppSettings.storage.cipher

        // Get the multipart data from the request.
        val multipart: MultiPartData = call.receiveMultipart()

        // Audit the document upload operation.
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "upload", ownerId = ownerId, groupId = groupId, log = "type=$type | cipher=$cipher")

        // Upload the document to the storage.
        val uploadManager: UploadManager = call.scope.get<UploadManager> { parametersOf(sessionContext) }
        val createdDocuments: List<Document> = uploadManager.upload(
            ownerId = ownerId,
            groupId = groupId,
            type = type,
            multipart = multipart,
            uploadRoot = AppSettings.storage.uploadsRootPath,
            cipher = cipher
        )

        // Respond with the created document details.
        if (createdDocuments.isEmpty()) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = "Invalid request."
            )
        } else {
            call.respond(
                status = HttpStatusCode.Created,
                message = createdDocuments
            )
        }
    }
}
