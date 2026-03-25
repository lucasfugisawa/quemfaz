package com.fugisawa.quemfaz.integration.profile

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileUpdateIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> =
        listOf(
            ProfessionalProfilesTable,
            UserPhoneAuthIdentitiesTable,
            UsersTable,
            OtpChallengesTable,
        )

    @Test
    fun `updating profile can change services`() =
        integrationTestApplication {
            // Set up user: auth + name + photo (required before confirming a profile)
            val token = obtainAuthToken("+5516900000070")
            completeNameStep(token, "Test User")
            setUserPhoto(token, "/api/images/profileupdatetestphoto0001")
            val client = createTestClient(token)

            // Create a profile with initial service
            client.post("/professional-profile/confirm") {
                contentType(ContentType.Application.Json)
                setBody(
                    ConfirmProfessionalProfileRequest(
                        description = "Pintor residencial",
                        selectedServiceIds = listOf("paint-residential"),
                        cityName = "Franca",
                        portfolioPhotoUrls = emptyList(),
                    ),
                )
            }

            // Update with an additional service
            val updateResponse =
                client.put("/professional-profile/me") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ConfirmProfessionalProfileRequest(
                            description = "Pintor residencial",
                            selectedServiceIds = listOf("paint-residential", "paint-commercial"),
                            cityName = "Franca",
                            portfolioPhotoUrls = emptyList(),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, updateResponse.status)
            val updated = updateResponse.body<ProfessionalProfileResponse>()
            assertEquals(2, updated.services.size)
            assertTrue(updated.services.any { it.serviceId == "paint-commercial" })
        }
}
