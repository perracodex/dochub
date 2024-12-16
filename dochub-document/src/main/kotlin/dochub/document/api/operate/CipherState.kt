/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api.operate

import dochub.base.context.SessionContext
import dochub.base.context.getContext
import dochub.document.api.DocumentRouteApi
import dochub.document.service.DocumentAuditService
import dochub.document.service.manager.CipherStateHandler
import io.github.perracodex.kopapi.dsl.operation.api
import io.github.perracodex.kopapi.dsl.parameter.pathParameter
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteApi
internal fun Route.changeDocumentsCipherStateRoute() {
    put("/v1/document/cipher/{cipher}") {
        val cipher: Boolean = call.parameters.getOrFail<Boolean>(name = "cipher")

        val sessionContext: SessionContext = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(sessionContext) }
            .audit(operation = "change cipher state", log = cipher.toString())

        val cipherStateHandler: CipherStateHandler = call.scope.get<CipherStateHandler> { parametersOf(sessionContext) }
        val count: Int = cipherStateHandler.changeState(cipher = cipher)
        call.respond(status = HttpStatusCode.OK, message = count)
    } api {
        tags = setOf("Document")
        summary = "Change the cipher state of all documents."
        description = "Change the cipher state of all document entries."
        operationId = "changeDocumentsCipherState"
        pathParameter<Boolean>(name = "cipher") {
            description = "The new cipher state."
        }
        response(status = HttpStatusCode.OK) {
            description = "The number of documents affected."
        }
    }
}
