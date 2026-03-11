package com.fugisawa.quemfaz.integration.auth

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpResponse
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
import kotlin.test.assertNotNull

class AuthIntegrationTest : BaseIntegrationTest() {
    // Define tables that should be cleaned before each test to ensure isolation
    override val tablesToClean: List<Table> =
        listOf(
            UserPhoneAuthIdentitiesTable,
            UsersTable,
            OtpChallengesTable,
        )

    @Test
    fun `should start otp process successfully`() =
        integrationTestApplication {
            val client = createTestClient()
            val phoneNumber = "+5511999999999"

            val response =
                client.post("/auth/start-otp") {
                    contentType(ContentType.Application.Json)
                    setBody(StartOtpRequest(phoneNumber = phoneNumber))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<StartOtpResponse>()
            assertEquals(true, body.success)
        }

    @Test
    fun `should fail to access protected me endpoint without token`() =
        integrationTestApplication {
            val client = createTestClient()
            val response =
                client.post("/auth/profile") {
                    contentType(ContentType.Application.Json)
                    // Request body doesn't matter much as we expect 401
                }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
