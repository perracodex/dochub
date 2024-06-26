/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.SessionContext
import kdoc.base.persistence.serializers.SUUID
import kdoc.base.persistence.utils.toUUID
import kdoc.base.persistence.utils.toUUIDOrNull
import kdoc.base.settings.AppSettings
import kdoc.document.entity.DocumentEntity
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentStorageService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.uploadDocumentsRoute() {
    // Upload a new document.
    post("{owner_id?}/{group_id?}/{type?}/{cipher?}") {
        val ownerId: SUUID = call.request.queryParameters["owner_id"].toUUID()
        val groupId: SUUID? = call.request.queryParameters["group_id"].toUUIDOrNull()
        val type: DocumentType = DocumentType.parse(value = call.request.queryParameters["type"]!!)
        val cipher: Boolean = call.request.queryParameters["cipher"]?.toBoolean()
            ?: AppSettings.storage.cipher

        // Get the multipart data from the request.
        val multipart: MultiPartData = call.receiveMultipart()

        // Audit the document upload operation.
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "upload", ownerId = ownerId, groupId = groupId, log = "type=$type | cipher=$cipher")

        // Upload the document to the storage.
        val storageService: DocumentStorageService = call.scope.get<DocumentStorageService> { parametersOf(sessionContext) }
        val createdDocuments: List<DocumentEntity> = storageService.upload(
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
