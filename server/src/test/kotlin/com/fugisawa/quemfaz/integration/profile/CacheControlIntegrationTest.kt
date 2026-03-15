package com.fugisawa.quemfaz.integration.profile

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CacheControlIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        ProfessionalProfilesTable,
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
    )

    @Test
    fun `GET public profile returns Cache-Control public header`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000060")
        completeNameStep(token, "Cache", "Test")
        setUserPhoto(token, "https://example.com/photo.jpg")
        createAndConfirmProfile(token)

        val authedClient = createTestClient(token)
        val myProfile = authedClient.get("/professional-profile/me").body<ProfessionalProfileResponse>()

        val publicClient = createTestClient()
        val response = publicClient.get("/professional-profile/${myProfile.id}")
        assertEquals("public, max-age=120", response.headers["Cache-Control"])
    }

    @Test
    fun `GET own profile returns Cache-Control private no-cache header`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000061")
        completeNameStep(token, "Cache", "Private")
        setUserPhoto(token, "https://example.com/photo.jpg")
        createAndConfirmProfile(token)

        val authedClient = createTestClient(token)
        val response = authedClient.get("/professional-profile/me")
        assertEquals("private, no-cache", response.headers["Cache-Control"])
    }
}
