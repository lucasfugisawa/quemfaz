package com.fugisawa.quemfaz.integration.search

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchPaginationIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> =
        listOf(
            ProfessionalProfilesTable,
            UserPhoneAuthIdentitiesTable,
            UsersTable,
            OtpChallengesTable,
        )

    @Test
    fun `search response includes pagination metadata`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511900000031")
            completeNameStep(token, "Test Painter")
            createAndConfirmProfile(token)

            val client = createTestClient()
            val response =
                client
                    .post("/search/professionals") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            SearchProfessionalsRequest(
                                query = "pintor",
                                cityId = "franca",
                                inputMode = InputMode.TEXT,
                                page = 0,
                                pageSize = 10,
                            ),
                        )
                    }.body<SearchProfessionalsResponse>()

            assertEquals(0, response.page)
            assertEquals(10, response.pageSize)
            assertTrue(response.totalCount >= 0)
            assertTrue(response.results.size <= response.pageSize)
        }

    @Test
    fun `page beyond total count returns empty results`() =
        integrationTestApplication {
            val client = createTestClient()
            val response =
                client
                    .post("/search/professionals") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            SearchProfessionalsRequest(
                                query = "pintor",
                                cityId = "franca",
                                inputMode = InputMode.TEXT,
                                page = 999,
                                pageSize = 10,
                            ),
                        )
                    }.body<SearchProfessionalsResponse>()

            assertEquals(999, response.page)
            assertEquals(0, response.results.size)
        }
}
