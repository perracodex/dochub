/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.document.api

import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import kdochub.core.plugins.RateLimitScope
import kdochub.core.settings.AppSettings
import kdochub.document.api.delete.deleteAllDocumentsRoute
import kdochub.document.api.delete.deleteDocumentByIdRoute
import kdochub.document.api.delete.deleteDocumentsByGroupRoute
import kdochub.document.api.fetch.*
import kdochub.document.api.operate.*

/**
 * Document related endpoints.
 *
 * #### References
 * - [Application Structure](https://ktor.io/docs/server-application-structure.html) for examples
 * of how to organize routes in diverse ways.
 */
@OptIn(DocumentRouteApi::class)
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

/**
 * Annotation for controlled access to the Document Routes API.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Only to be used within the Document Routes API.")
@Retention(AnnotationRetention.BINARY)
internal annotation class DocumentRouteApi
