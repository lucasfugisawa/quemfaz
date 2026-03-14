package com.fugisawa.quemfaz.integration.auth

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.infrastructure.images.StoredImagesTable
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SetProfilePhotoIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
        StoredImagesTable,
    )

    // obtainAuthToken and completeNameStep are inherited from BaseIntegrationTest

    @Test
    fun `should set photo URL when given a valid internal URL`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000010")
        completeNameStep(token, "João", "Silva")
        val client = createTestClient(token)

        val response = client.post("/auth/photo") {
            contentType(ContentType.Application.Json)
            setBody(SetProfilePhotoRequest(photoUrl = "/api/images/01ABCDEF1234567890ABCDEF"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<UserProfileResponse>()
        assertEquals("/api/images/01ABCDEF1234567890ABCDEF", body.photoUrl)
    }

    @Test
    fun `should reject arbitrary external URL`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000011")
        completeNameStep(token, "Ana", "Souza")
        val client = createTestClient(token)

        val response = client.post("/auth/photo") {
            contentType(ContentType.Application.Json)
            setBody(SetProfilePhotoRequest(photoUrl = "https://evil.com/hack.png"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
