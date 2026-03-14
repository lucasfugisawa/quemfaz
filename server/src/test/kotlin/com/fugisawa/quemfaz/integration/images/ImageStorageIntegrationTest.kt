package com.fugisawa.quemfaz.integration.images

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.infrastructure.images.StoredImagesTable
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// NOTE: StoredImagesTable is defined in DatabaseImageStorageService.kt
// obtainAuthToken is inherited from BaseIntegrationTest (added in Task 7, Step 7)

class ImageStorageIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
        StoredImagesTable,
    )

    @Test
    fun `should upload image and retrieve it by URL`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000001")
        val client = createTestClient(token)

        val imageBytes = ByteArray(100) { it.toByte() }

        val uploadResponse = client.submitFormWithBinaryData(
            url = "/api/images/upload",
            formData = formData {
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"test.png\"")
                })
            }
        )
        assertEquals(HttpStatusCode.OK, uploadResponse.status)

        val body = uploadResponse.bodyAsText()
        assertTrue(body.contains("/api/images/"))

        val url = kotlinx.serialization.json.Json.decodeFromString<com.fugisawa.quemfaz.contract.image.UploadImageResponse>(body).url
        val imageId = url.removePrefix("/api/images/")

        val getResponse = client.get("/api/images/$imageId")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertEquals("image/png", getResponse.headers[HttpHeaders.ContentType])
        val returnedBytes = getResponse.bodyAsBytes()
        assertEquals(100, returnedBytes.size)
    }

    @Test
    fun `should reject upload with disallowed MIME type`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000002")
        val client = createTestClient(token)

        val response = client.submitFormWithBinaryData(
            url = "/api/images/upload",
            formData = formData {
                append("image", ByteArray(10), Headers.build {
                    append(HttpHeaders.ContentType, "image/gif")
                    append(HttpHeaders.ContentDisposition, "filename=\"bad.gif\"")
                })
            }
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should reject upload exceeding 5MB`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000003")
        val client = createTestClient(token)

        val tooBig = ByteArray(5 * 1024 * 1024 + 1)

        val response = client.submitFormWithBinaryData(
            url = "/api/images/upload",
            formData = formData {
                append("image", tooBig, Headers.build {
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"big.png\"")
                })
            }
        )
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should return 404 for unknown image ID`() = integrationTestApplication {
        val client = createTestClient()
        val response = client.get("/api/images/nonexistent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
