/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.persistence.pagination.Page
import kdoc.document.entity.DocumentEntity
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import kdoc.document.service.DocumentStorage
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.backupDocumentsRoute() {
    // Downloads a backup file containing all the documents.
    get("backup") {
        // Audit the backup.
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        val auditService: DocumentAuditService = call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
        auditService.audit(operation = "backup")

        // Get all documents.
        val documentService: DocumentService = call.scope.get<DocumentService> { parametersOf(sessionContext) }
        val documents: Page<DocumentEntity> = documentService.findAll()
        if (documents.totalElements == 0) {
            call.respond(status = HttpStatusCode.NoContent, message = "No documents found.")
            return@get
        }

        // Stream the backup to the client.
        val storageService: DocumentStorage = call.scope.get<DocumentStorage> { parametersOf(sessionContext) }
        DocumentStorage.backupCountMetric.increment()
        storageService.streamZip(call = call, filename = "backup", documents = documents.content, decipher = false)
    }
}
