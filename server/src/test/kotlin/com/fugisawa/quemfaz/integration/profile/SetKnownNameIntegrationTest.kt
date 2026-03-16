package com.fugisawa.quemfaz.integration.profile

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.profile.SetKnownNameRequest
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SetKnownNameIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        ProfessionalProfilesTable,
        UserPhoneAuthIdentitiesTable,
        UsersTable,
        OtpChallengesTable,
    )

    @Test
    fun `should set known name on existing profile`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000010")
        completeNameStep(token, "Lucas Test")
        setUserPhoto(token, "/api/images/test-photo-id")
        createAndConfirmProfile(token)

        val client = createTestClient(token)
        val response = client.put("/professional-profile/known-name") {
            contentType(ContentType.Application.Json)
            setBody(SetKnownNameRequest(knownName = "Lu"))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `should return 404 when no profile exists`() = integrationTestApplication {
        val token = obtainAuthToken("+5511900000011")
        completeNameStep(token, "Lucas Test")

        val client = createTestClient(token)
        val response = client.put("/professional-profile/known-name") {
            contentType(ContentType.Application.Json)
            setBody(SetKnownNameRequest(knownName = "Lu"))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
