/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing

import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kdoc.base.plugins.RateLimitScope
import kdoc.base.settings.AppSettings
import kdoc.document.routing.delete.deleteAllDocumentsRoute
import kdoc.document.routing.delete.deleteDocumentByIdRoute
import kdoc.document.routing.delete.deleteDocumentsByGroupRoute
import kdoc.document.routing.get.*
import kdoc.document.routing.operate.*

/**
 * Annotation for controlled access to the Document Routes API.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used within the Document Routes API.")
@Retention(AnnotationRetention.BINARY)
annotation class DocumentRouteAPI

/**
 * Document related endpoints.
 *
 * See [Application Structure](https://ktor.io/docs/server-application-structure.html) for examples
 * of how to organize routes in diverse ways.
 */
@OptIn(DocumentRouteAPI::class)
fun Route.documentRoute() {

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PUBLIC_API.key)) {
        authenticate(AppSettings.security.jwtAuth.providerName, optional = !AppSettings.security.isEnabled) {
            route("v1/document") {
                uploadDocumentsRoute()

                searchDocumentsRoute()
                findDocumentsByOwnerRoute()

                findDocumentsByGroupRoute()
                deleteDocumentsByGroupRoute()

                getDocumentSignedUrlRoute()
                downloadDocumentRoute()

                route("{document_id}") {
                    findDocumentByIdRoute()
                    deleteDocumentByIdRoute()
                }
            }
        }
    }

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
        authenticate(AppSettings.security.jwtAuth.providerName, optional = !AppSettings.security.isEnabled) {
            route("v1/document") {
                changeDocumentsCipherStateRoute()
                backupDocumentsRoute()
                findAllDocumentsRoute()
                deleteAllDocumentsRoute()
            }
        }
    }
}
