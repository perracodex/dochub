/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api.operate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kdoc.base.env.CallContext
import kdoc.base.env.CallContext.Companion.getContext
import kdoc.document.api.DocumentRouteAPI
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.managers.CipherStateHandler
import org.koin.core.parameter.parametersOf
import org.koin.ktor.plugin.scope

@DocumentRouteAPI
internal fun Route.changeDocumentsCipherStateRoute() {
    /**
     * Change the cipher state of all documents.
     * @OpenAPITag Document - Operate
     */
    put("v1/document/cipher/{cipher}") {
        val cipher: Boolean = call.parameters.getOrFail<Boolean>(name = "cipher")

        val callContext: CallContext? = call.getContext()
        call.scope.get<DocumentAuditService> { parametersOf(callContext) }
            .audit(operation = "change cipher state", log = cipher.toString())

        val cipherStateHandler: CipherStateHandler = call.scope.get<CipherStateHandler> { parametersOf(callContext) }
        val count: Int = cipherStateHandler.changeState(cipher = cipher)
        call.respond(status = HttpStatusCode.OK, message = count)
    }
}
