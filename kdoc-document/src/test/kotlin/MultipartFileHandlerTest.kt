/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import junit.framework.TestCase.assertTrue
import kdoc.access.actor.di.ActorDomainInjection
import kdoc.access.rbac.di.RbacDomainInjection
import kdoc.base.database.schema.document.types.DocumentType
import kdoc.base.utils.TestUtils
import kdoc.document.di.DocumentDomainInjection
import kdoc.document.service.managers.upload.MultipartFileHandler
import kdoc.document.service.managers.upload.annotation.UploadAPI
import java.io.File
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class MultipartFileHandlerTest {

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

    @OptIn(UploadAPI::class)
    @Test
    fun testUpload(): Unit = testApplication {
        val numberOfFiles = 10
        val tempUploadPath: File = createTempDirectory().toFile()
        val filenamePrefix = "testfile"
        val descriptionPrefix = "Test file description"

        try {
            application {
                routing {
                    post("/test-endpoint") {
                        val multipart: MultiPartData = call.receiveMultipart()
                        val ownerId: UUID = UUID.randomUUID()
                        val groupId: UUID = UUID.randomUUID()
                        val type: DocumentType = DocumentType.GENERAL

                        val persistedFiles: List<MultipartFileHandler.Response> = MultipartFileHandler(
                            uploadsRoot = tempUploadPath.absolutePath,
                            cipher = true
                        ).receive(
                            ownerId = ownerId,
                            groupId = groupId,
                            type = type,
                            multipart = multipart
                        )

                        assertNotNull(actual = persistedFiles)
                        assertEquals(expected = numberOfFiles, actual = persistedFiles.size)

                        persistedFiles.forEach { fileDetails ->
                            assertTrue(fileDetails.originalFilename.startsWith(filenamePrefix))
                            assertTrue(fileDetails.description!!.startsWith(descriptionPrefix))
                            assertNotEquals(illegal = fileDetails.originalFilename, actual = fileDetails.storageFilename)
                        }

                        call.respondText(text = "Request processed.", status = HttpStatusCode.OK)
                    }
                }
            }

            val client: HttpClient = createClient {
                this@testApplication.install(ContentNegotiation) {
                    json()
                }
            }

            val formData: List<PartData> = formData {
                repeat(times = numberOfFiles) { index ->
                    val description = "$descriptionPrefix ($index)"
                    val filename = "$filenamePrefix-$index.txt"
                    append(
                        key = description,
                        value = ByteArray(10) { it.toByte() },  // Example byte array
                        headers = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file-$index\"; filename=\"$filename\""
                            )
                        }
                    )
                }
            }

            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "/test-endpoint",
                formData = formData
            )

            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            assertEquals(expected = "Request processed.", actual = response.bodyAsText())
        } finally {
            tempUploadPath.deleteRecursively()
        }
    }
}
