/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.api

import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kdoc.base.plugins.RateLimitScope
import kdoc.base.settings.AppSettings
import kdoc.document.api.delete.deleteAllDocumentsRoute
import kdoc.document.api.delete.deleteDocumentByIdRoute
import kdoc.document.api.delete.deleteDocumentsByGroupRoute
import kdoc.document.api.fetch.*
import kdoc.document.api.operate.*

/**
 * Annotation for controlled access to the Document Routes API.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used within the Document Routes API.")
@Retention(AnnotationRetention.BINARY)
internal annotation class DocumentRouteAPI

/**
 * Document related endpoints.
 *
 * See [Application Structure](https://ktor.io/docs/server-application-structure.html) for examples
 * of how to organize routes in diverse ways.
 */
@OptIn(DocumentRouteAPI::class)
public fun Route.documentRoutes() {

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PUBLIC_API.key)) {
        authenticate(AppSettings.security.jwtAuth.providerName, optional = !AppSettings.security.isEnabled) {
            uploadDocumentsRoute()

            searchDocumentsRoute()
            findDocumentsByOwnerRoute()

            findDocumentsByGroupRoute()
            deleteDocumentsByGroupRoute()

            getDocumentSignedUrlRoute()
            downloadDocumentRoute()

            findDocumentByIdRoute()
            deleteDocumentByIdRoute()
        }
    }

    rateLimit(configuration = RateLimitName(name = RateLimitScope.PRIVATE_API.key)) {
        authenticate(AppSettings.security.jwtAuth.providerName, optional = !AppSettings.security.isEnabled) {
            changeDocumentsCipherStateRoute()
            backupDocumentsRoute()
            findAllDocumentsRoute()
            deleteAllDocumentsRoute()
        }
    }
}
