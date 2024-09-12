/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import io.mockk.every
import io.mockk.mockk
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.SessionContext
import kdoc.base.persistence.serializers.OffsetTimestamp
import kdoc.base.utils.TestUtils
import kdoc.document.di.DocumentDomainInjection
import kdoc.document.entity.DocumentDto
import kdoc.document.entity.DocumentRequest
import kdoc.document.repository.IDocumentRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.test.*
import kotlin.uuid.Uuid


class TimestampTest : KoinComponent {

    @BeforeTest
    fun setUp() {
        TestUtils.loadSettings()
        TestUtils.setupDatabase()
        TestUtils.setupKoin(modules = listOf(DocumentDomainInjection.get()))
    }

    @AfterTest
    fun tearDown() {
        TestUtils.tearDown()
    }

    @Test
    fun testTimestamp(): Unit = testSuspend {
        val sessionContext: SessionContext = mockk<SessionContext>()
        every { sessionContext.schema } returns null

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

        val documentId: Uuid = documentRepository.create(documentRequest = documentRequest)
        val document: DocumentDto = documentRepository.findById(documentId = documentId)!!

        // Assert that the record has timestamps.
        assertNotNull(actual = document)
        assertNotNull(actual = document.meta.createdAt)
        assertNotNull(actual = document.meta.updatedAt)

        // Assert that both timestamps are the same after creation.
        assertEquals(
            expected = document.meta.createdAt,
            actual = document.meta.updatedAt
        )

        val createdAt: OffsetTimestamp = document.meta.createdAt
        val updatedAt: OffsetTimestamp = document.meta.updatedAt
        documentRepository.update(documentId = documentId, documentRequest = documentRequest)
        val updatedDocument: DocumentDto = documentRepository.findById(documentId = documentId)!!

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
