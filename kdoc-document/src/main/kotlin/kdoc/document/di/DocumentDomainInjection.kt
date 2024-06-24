/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.di

import kdoc.base.env.SessionContext
import kdoc.document.repository.DocumentAuditRepository
import kdoc.document.repository.DocumentRepository
import kdoc.document.repository.IDocumentAuditRepository
import kdoc.document.repository.IDocumentRepository
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import kdoc.document.service.DocumentStorage
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.RequestScope

/**
 * Document domain dependency injection module.
 */
object DocumentDomainInjection {

    /**
     * Get the dependency injection module for the Document domain.
     */
    fun get(): Module {
        return module {

            // Scoped definitions within RequestScope for single request lifecycle.
            // Services receive the session context as a parameter. Repositories,
            // which should only be accessed by services, do not receive it directly.

            scope<RequestScope> {

                scoped<IDocumentAuditRepository> {
                    DocumentAuditRepository(
                        sessionContext = get<SessionContext>()
                    )
                }

                scoped<DocumentAuditService> { parameters ->
                    DocumentAuditService(
                        sessionContext = parameters.get<SessionContext>(),
                        documentAuditRepository = get<IDocumentAuditRepository>()
                    )
                }

                scoped<IDocumentRepository> {
                    DocumentRepository(
                        sessionContext = get<SessionContext>()
                    )
                }

                scoped<DocumentService> { parameters ->
                    DocumentService(
                        sessionContext = parameters.get<SessionContext>(),
                        documentRepository = get<IDocumentRepository>()
                    )
                }

                scoped<DocumentStorage> { parameters ->
                    DocumentStorage(
                        sessionContext = parameters.get<SessionContext>(),
                        documentRepository = get<IDocumentRepository>()
                    )
                }
            }

            // Definitions for non-scoped (global) access.

            single<IDocumentAuditRepository> {
                DocumentAuditRepository(
                    sessionContext = get<SessionContext>()
                )
            }

            single<DocumentAuditService> {
                DocumentAuditService(
                    sessionContext = get<SessionContext>(),
                    documentAuditRepository = get<IDocumentAuditRepository>()
                )
            }

            single<IDocumentRepository> {
                DocumentRepository(
                    sessionContext = get<SessionContext>()
                )
            }

            single<DocumentService> {
                DocumentService(
                    sessionContext = get<SessionContext>(),
                    documentRepository = get<IDocumentRepository>()
                )
            }

            single<DocumentStorage> {
                DocumentStorage(
                    sessionContext = get<SessionContext>(),
                    documentRepository = get<IDocumentRepository>()
                )
            }
        }
    }
}
