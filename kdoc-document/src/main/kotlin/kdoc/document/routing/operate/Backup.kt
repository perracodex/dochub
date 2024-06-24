/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kdoc.base.env.SessionContext
import kdoc.base.utils.DateTimeUtils
import kdoc.base.utils.KLocalDateTime
import kdoc.document.routing.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import java.io.PipedInputStream
import java.io.PipedOutputStream

@DocumentRouteAPI
internal fun Route.backupAllDocuments() {
    // Downloads a backup file containing all the documents.
    get("backup") {
        // Audit the backup.
        val sessionContext: SessionContext? = SessionContext.from(call = call)
        val auditService: DocumentAuditService = call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
        auditService.audit(operation = "backup")

        // Stream the backup to the client.
        streamBackup(call = call, sessionContext = sessionContext)
    }
}

private suspend fun streamBackup(call: ApplicationCall, sessionContext: SessionContext?) = withContext(Dispatchers.IO) {
    DocumentStorage.backupCountMetric.increment()

    val currentTime: KLocalDateTime = DateTimeUtils.currentUTCDateTime()
    val filename = "backup_$currentTime.zip"
    call.response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(
            ContentDisposition.Parameters.FileName,
            filename
        ).toString()
    )

    val service: DocumentStorage = call.scope.get<DocumentStorage> { parametersOf(sessionContext) }
    val pipedOutputStream = PipedOutputStream()
    val pipedInputStream = PipedInputStream(pipedOutputStream)

    // Launch a coroutine to handle the backup and streaming.
    launch(Dispatchers.IO) {
        pipedOutputStream.use { outputStream ->
            service.backup(outputStream = outputStream)
        }
    }

    call.respondOutputStream(contentType = ContentType.Application.OctetStream) {
        // Stream the content from pipedInputStream to the response outputStream.
        pipedInputStream.use { inputStream ->
            inputStream.copyTo(this)
        }
    }
}
