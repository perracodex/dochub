/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.document.di

import kdoc.document.repository.DocumentAuditRepository
import kdoc.document.repository.DocumentRepository
import kdoc.document.repository.IDocumentAuditRepository
import kdoc.document.repository.IDocumentRepository
import kdoc.document.service.DocumentAuditService
import kdoc.document.service.DocumentService
import kdoc.document.service.managers.CipherStateHandler
import kdoc.document.service.managers.upload.UploadManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
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
            // Services receive the SessionContext as a parameter. Repositories,
            // which should only be accessed by services, do not receive it directly.

            scope<RequestScope> {
                scopedOf(::DocumentAuditRepository) {
                    bind<IDocumentAuditRepository>()
                }

                scopedOf(::DocumentAuditService)

                scopedOf(::DocumentRepository) {
                    bind<IDocumentRepository>()
                }

                scopedOf(::DocumentService)
                scopedOf(::UploadManager)
                scopedOf(::CipherStateHandler)
            }

            // Definitions for non-scoped (global) access.

            factoryOf(::DocumentAuditRepository) {
                bind<IDocumentAuditRepository>()
            }

            factoryOf(::DocumentAuditService)

            factoryOf(::DocumentRepository) {
                bind<IDocumentRepository>()
            }

            factoryOf(::DocumentService)
            factoryOf(::DocumentAuditService)
            factoryOf(::CipherStateHandler)
        }
    }
}
