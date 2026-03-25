package com.fugisawa.quemfaz.integration.profile

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnboardingFallbackIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> =
        listOf(
            ProfessionalProfilesTable,
            UserPhoneAuthIdentitiesTable,
            UsersTable,
            OtpChallengesTable,
        )

    @Test
    fun `draft creation returns services even when LLM fails (local matching fallback)`() =
        integrationTestApplication {
            // Note: In integration tests, OPENAI_API_KEY is not set, so LLM calls fail.
            // This tests the real fallback behavior.
            val token = obtainAuthToken("+5511900000060")

            val authedClient = createTestClient(token)
            val response =
                authedClient.post("/professional-profile/draft") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProfessionalProfileDraftRequest("Sou eletricista", InputMode.TEXT))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val draft = response.body<CreateProfessionalProfileDraftResponse>()

            assertTrue(draft.llmUnavailable, "Should flag LLM as unavailable")
            assertTrue(draft.interpretedServices.isNotEmpty(), "Should have locally matched services")
            assertTrue(draft.followUpQuestions.isEmpty(), "Should not trigger clarification on LLM failure")
        }

    @Test
    fun `draft response includes editedDescription field`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511900000061")
            val authedClient = createTestClient(token)
            val response =
                authedClient.post("/professional-profile/draft") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProfessionalProfileDraftRequest("faço pintura de casa e comércio", InputMode.TEXT))
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val draft = response.body<CreateProfessionalProfileDraftResponse>()
            assertTrue(draft.editedDescription.isNotBlank(), "editedDescription should not be blank")
        }
}
