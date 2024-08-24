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
import kdoc.document.service.managers.CipherStateHandler
import kdoc.document.service.managers.upload.UploadManager
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.RequestScope

/**
 * Document domain dependency injection module.
 */
public object DocumentDomainInjection {

    /**
     * Get the dependency injection module for the Document domain.
     */
    public fun get(): Module {
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

                scoped<UploadManager> { parameters ->
                    UploadManager(
                        sessionContext = parameters.get<SessionContext>(),
                        documentRepository = get<IDocumentRepository>()
                    )
                }

                scoped<CipherStateHandler> { parameters ->
                    CipherStateHandler(
                        sessionContext = parameters.get<SessionContext>(),
                        documentRepository = get<IDocumentRepository>()
                    )
                }
            }

            // Definitions for non-scoped (global) access.

            factory<IDocumentAuditRepository> {
                DocumentAuditRepository(
                    sessionContext = get<SessionContext>()
                )
            }

            factory<DocumentAuditService> { parameters ->
                DocumentAuditService(
                    sessionContext = parameters.get<SessionContext>(),
                    documentAuditRepository = get<IDocumentAuditRepository>()
                )
            }

            factory<IDocumentRepository> {
                DocumentRepository(
                    sessionContext = get<SessionContext>()
                )
            }

            factory<DocumentService> { parameters ->
                DocumentService(
                    sessionContext = parameters.get<SessionContext>(),
                    documentRepository = get<IDocumentRepository>()
                )
            }

            factory<UploadManager> { parameters ->
                UploadManager(
                    sessionContext = parameters.get<SessionContext>(),
                    documentRepository = get<IDocumentRepository>()
                )
            }

            factory<CipherStateHandler> { parameters ->
                CipherStateHandler(
                    sessionContext = parameters.get<SessionContext>(),
                    documentRepository = get<IDocumentRepository>()
                )
            }
        }
    }
}
