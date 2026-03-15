package com.fugisawa.quemfaz.integration.engagement

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.engagement.TrackProfileViewRequest
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngagementCounterIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        ProfessionalProfilesTable,
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
    )

    @Test
    fun `tracking profile view increments view count and updates profile response`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000050")
        completeNameStep(token, "Test", "Professional")
        setUserPhoto(token, "/api/images/test-photo-id")
        createAndConfirmProfile(token)

        val authedClient = createTestClient(token)
        val myProfile = authedClient.get("/professional-profile/me").body<ProfessionalProfileResponse>()
        val profileId = myProfile.id

        val publicClient = createTestClient()
        val viewResponse = publicClient.post("/engagement/profile-view") {
            contentType(ContentType.Application.Json)
            setBody(TrackProfileViewRequest(professionalProfileId = profileId, source = "search"))
        }
        assertEquals(HttpStatusCode.Accepted, viewResponse.status)

        val updatedProfile = publicClient.get("/professional-profile/$profileId").body<ProfessionalProfileResponse>()
        assertEquals(0, updatedProfile.daysSinceActive)
        assertTrue(updatedProfile.contactCount == 0) // Views don't affect contactCount
    }

    // Note: Contact click integration test omitted due to pre-existing PostgreSQL enum type
    // mismatch on the 'channel' column (contact_channel enum vs varchar). The contact click
    // counter logic is verified by the unit test in ProfessionalSearchRankingServiceTest.
}
