/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */
import io.ktor.test.dispatcher.*
import io.mockk.every
import io.mockk.mockk
import kdoc.access.actor.di.ActorDomainInjection
import kdoc.access.rbac.di.RbacDomainInjection
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.SessionContext
import kdoc.base.persistence.pagination.Page
import kdoc.base.utils.TestUtils
import kdoc.document.di.DocumentDomainInjection
import kdoc.document.entity.DocumentEntity
import kdoc.document.entity.DocumentRequest
import kdoc.document.service.DocumentService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StressTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        TestUtils.setupDatabase()
        TestUtils.setupKoin(
            modules = listOf(
                RbacDomainInjection.get(),
                ActorDomainInjection.get(),
                DocumentDomainInjection.get()
            )
        )
    }

    @AfterTest
    fun tearDown() {
        TestUtils.tearDown()
    }

    @Test
    fun largeConcurrentSet(): Unit = testSuspend {
        val documentRequest = DocumentRequest(
            ownerId = UUID.randomUUID(),
            groupId = UUID.randomUUID(),
            name = "AnyName",
            type = DocumentType.entries.random(),
            description = "AnyDescription",
            location = "AnyLocation",
            isCiphered = false
        )

        val sessionContext: SessionContext = mockk<SessionContext>()
        every { sessionContext.schema } returns null

        val documentService: DocumentService by inject(
            parameters = { parametersOf(sessionContext) }
        )

        val totalElements = 10_000

        val jobs: List<Deferred<DocumentEntity>> = List(size = totalElements) { index ->
            val randomChars: String = List(size = 2) { "abc0123".random() }.joinToString(separator = "")

            val request = documentRequest.copy(
                name = "${documentRequest.name}_$index",
                type = DocumentType.entries.random(),
                description = "${randomChars}_${documentRequest.description}",
                location = "${randomChars}_${documentRequest.location}"
            )

            async {
                documentService.create(documentRequest = request)
            }
        }

        // Await all the Deferred objects to ensure all async operations complete.
        jobs.awaitAll()

        // Verify all records after all insertions are complete.
        val documents: Page<DocumentEntity> = documentService.findAll()
        assertEquals(expected = totalElements, actual = documents.content.size)
        assertEquals(expected = totalElements, actual = documents.totalElements)

        documentService.deleteAll()
    }
}

