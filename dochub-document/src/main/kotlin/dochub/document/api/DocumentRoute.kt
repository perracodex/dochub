/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.document.api

import dochub.base.plugins.RateLimitScope
import dochub.base.settings.AppSettings
import dochub.document.api.delete.deleteAllDocumentsRoute
import dochub.document.api.delete.deleteDocumentByIdRoute
import dochub.document.api.delete.deleteDocumentsByGroupRoute
import dochub.document.api.fetch.*
import dochub.document.api.operate.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*

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
