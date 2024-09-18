/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.delete

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kdoc.base.env.CallContext
import kdoc.base.env.CallContext.Companion.getContext
import kdoc.base.persistence.utils.toUuid
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope
import kotlin.uuid.Uuid

@DocumentRouteAPI
internal fun Route.deleteDocumentsByGroupRoute() {
    /**
     * Delete all documents by group.
     * @OpenAPITag Document - Delete
     */
    delete("v1/document/group/{group_id}") {
        val groupId: Uuid = call.parameters.getOrFail(name = "group_id").toUuid()

        val callContext: CallContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(callContext) }
            .audit(operation = "delete by group", groupId = groupId)

        val service: DocumentService = call.scope.get<DocumentService> { parametersOf(callContext) }
        val deletedCount: Int = service.deleteByGroup(groupId = groupId)
        call.respond(status = HttpStatusCode.OK, message = deletedCount)
    }
}