/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.operate

import dochub.base.context.SessionContext
import dochub.base.context.sessionContext
import dochub.base.settings.AppSettings
import dochub.base.util.toUuid
import dochub.base.util.toUuidOrNull
import dochub.database.schema.document.type.DocumentType
import dochub.document.api.DocumentRouteApi
import dochub.document.model.Document
import dochub.document.service.DocumentAuditService
import dochub.document.service.manager.upload.UploadManager
import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.queryParameter
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteApi
internal fun Route.uploadDocumentsRoute() {
    post("/v1/document/") {
        // Get the owner ID, group ID, document type, and cipher flag from the query parameters.
        val ownerId: Uuid = call.request.queryParameters.getOrFail(name = "owner_id").toUuid()
        val groupId: Uuid? = call.request.queryParameters["group_id"].toUuidOrNull()
        val type: DocumentType = DocumentType.parse(value = call.request.queryParameters.getOrFail(name = "type"))
        val cipher: Boolean = call.request.queryParameters["cipher"]?.toBoolean()
            ?: AppSettings.storage.cipher

        // Audit the attempt operation.
        val sessionContext: SessionContext = call.sessionContext
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "upload", ownerId = ownerId, groupId = groupId, log = "type=$type | cipher=$cipher")

        // Get the multipart data from the request.
        val multipart: MultiPartData = call.receiveMultipart()

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
    } api {
        tags = setOf("Document")
        summary = "Upload a new document."
        description = "Upload a new document to the storage."
        operationId = "uploadDocuments"
        queryParameter<Uuid>(name = "owner_id") {
            description = "The owner ID of the document."
        }
        queryParameter<Uuid>(name = "group_id") {
            description = "The group ID of the document."
            required = false
        }
        queryParameter<String>(name = "type") {
            description = "The type of the document."
        }
        queryParameter<Boolean>(name = "cipher") {
            description = "Whether to encrypt the document."
            required = false
        }
        requestBody<Unit> {
            description = "The document file to upload."
            multipart {
                part<PartData.FileItem>("file") {
                    description = "The file to upload."
                }
            }
        }
        response(status = HttpStatusCode.Created) {
            description = "The created document details."
        }
        response(status = HttpStatusCode.BadRequest) {
            description = "Invalid request."
        }
    }
}
