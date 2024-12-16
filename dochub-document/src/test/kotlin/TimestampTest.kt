/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import dochub.base.context.SessionContext
import dochub.base.util.TestUtils
import dochub.database.schema.document.type.DocumentType
import dochub.database.test.DatabaseTestUtils
import dochub.document.di.DocumentDomainInjection
import dochub.document.model.Document
import dochub.document.model.DocumentRequest
import dochub.document.repository.IDocumentRepository
import io.ktor.test.dispatcher.*
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.test.*
import kotlin.uuid.Uuid

class TimestampTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        DatabaseTestUtils.setupDatabase()
        TestUtils.setupKoin(modules = listOf(DocumentDomainInjection.get()))
    }

    @AfterTest
    fun tearDown() {
        DatabaseTestUtils.closeDatabase()
        TestUtils.tearDown()
    }

    @Test
    fun testTimestamp(): Unit = testSuspend {
        val sessionContext: SessionContext = mockk<SessionContext>()

        val documentRepository: IDocumentRepository by inject(
            parameters = { parametersOf(sessionContext) }
        )

        val documentRequest = DocumentRequest(
            ownerId = Uuid.random(),
            groupId = Uuid.random(),
            type = DocumentType.entries.random(),
            description = "ANyDescription",
            originalName = "AnyName",
            storageName = "AnyName",
            location = "AnyLocation",
            isCiphered = false,
            size = 0
        )

        val document: Document = documentRepository.create(request = documentRequest)

        // Assert that both timestamps are the same after creation.
        assertEquals(
            expected = document.meta.createdAt,
            actual = document.meta.updatedAt
        )

        val createdAt: Instant = document.meta.createdAt
        val updatedAt: Instant = document.meta.updatedAt
        val updatedDocument: Document? = documentRepository.update(
            documentId = document.id,
            request = documentRequest
        )
        assertNotNull(updatedDocument)

        // The createdAt timestamp should not change.
        assertEquals(
            expected = createdAt,
            actual = updatedDocument.meta.createdAt
        )

        // The updatedAt timestamp should change.
        assertNotEquals(
            illegal = updatedAt,
            actual = updatedDocument.meta.updatedAt
        )
    }
}
