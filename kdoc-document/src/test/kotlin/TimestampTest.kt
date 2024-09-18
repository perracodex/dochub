/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.test.dispatcher.*
import io.mockk.every
import io.mockk.mockk
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.env.CallContext
import kdoc.base.persistence.serializers.OffsetTimestamp
import kdoc.base.utils.TestUtils
import kdoc.document.di.DocumentDomainInjection
import kdoc.document.model.Document
import kdoc.document.model.DocumentRequest
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
        val callContext: CallContext = mockk<CallContext>()
        every { callContext.schema } returns null

        val documentRepository: IDocumentRepository by inject(
            parameters = { parametersOf(callContext) }
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

        val createdAt: OffsetTimestamp = document.meta.createdAt
        val updatedAt: OffsetTimestamp = document.meta.updatedAt
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
