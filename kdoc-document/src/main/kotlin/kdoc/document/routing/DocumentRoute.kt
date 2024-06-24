/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.routing

import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kdoc.base.plugins.RateLimitScope
import kdoc.base.settings.AppSettings
import kdoc.document.routing.delete.deleteAllDocuments
import kdoc.document.routing.delete.deleteDocumentById
import kdoc.document.routing.delete.deleteDocumentsByGroup
import kdoc.document.routing.get.*
import kdoc.document.routing.operate.changeDocumentsCipherState
import kdoc.document.routing.operate.downloadDocument
import kdoc.document.routing.operate.getDocumentSignedUrl
import kdoc.document.routing.operate.uploadDocuments

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
                uploadDocuments()

                searchDocuments()
                findDocumentsByOwner()

                findDocumentsByGroup()
                deleteDocumentsByGroup()

                getDocumentSignedUrl()
                downloadDocument()

                route("{document_id}") {
                    findDocumentById()
                    deleteDocumentById()
                }
            }
        }
    }

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
        authenticate(AppSettings.security.jwtAuth.providerName, optional = !AppSettings.security.isEnabled) {
            route("v1/document") {
                changeDocumentsCipherState()
                findAllDocuments()
                deleteAllDocuments()
            }
        }
    }
}
