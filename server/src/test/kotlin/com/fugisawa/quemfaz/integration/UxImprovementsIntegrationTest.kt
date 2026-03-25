package com.fugisawa.quemfaz.integration

import com.fugisawa.quemfaz.auth.infrastructure.OtpChallengesTable
import com.fugisawa.quemfaz.auth.infrastructure.UserPhoneAuthIdentitiesTable
import com.fugisawa.quemfaz.auth.infrastructure.UsersTable
import com.fugisawa.quemfaz.contract.auth.UpdateDateOfBirthRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.contract.search.PopularServicesResponse
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilePortfolioPhotosTable
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfileServicesTable
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import com.fugisawa.quemfaz.search.infrastructure.persistence.SearchEventsTable
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UxImprovementsIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> =
        listOf(
            SearchEventsTable,
            ProfessionalProfilePortfolioPhotosTable,
            ProfessionalProfileServicesTable,
            ProfessionalProfilesTable,
            OtpChallengesTable,
            UserPhoneAuthIdentitiesTable,
            UsersTable,
        )

    // ── 1. fullName profile completion ──

    @Test
    fun `POST auth profile with full name should return 200`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990010")
            val client = createTestClient(token)

            val response =
                client.post("/auth/profile") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"fullName":"Maria da Silva"}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `POST auth profile with single word should return 400`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990011")
            val client = createTestClient(token)

            val response =
                client.post("/auth/profile") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"fullName":"Maria"}""")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST auth profile with three-word name should return 200`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990012")
            val client = createTestClient(token)

            val response =
                client.post("/auth/profile") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"fullName":"Maria da Silva Santos"}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    // ── 2. Date of birth endpoint ──

    @Test
    fun `PUT date of birth with valid adult date should return 200`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990020")
            completeNameStep(token, "Maria da Silva")
            val client = createTestClient(token)

            val response =
                client.put("/auth/me/date-of-birth") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateDateOfBirthRequest(dateOfBirth = "1990-05-15"))
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `PUT date of birth with underage date should return 422`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990021")
            completeNameStep(token, "Maria da Silva")
            val client = createTestClient(token)

            val response =
                client.put("/auth/me/date-of-birth") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateDateOfBirthRequest(dateOfBirth = "2015-01-01"))
                }
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        }

    @Test
    fun `GET auth me should return dateOfBirth after setting it`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990022")
            completeNameStep(token, "Maria da Silva")
            val client = createTestClient(token)

            client.put("/auth/me/date-of-birth") {
                contentType(ContentType.Application.Json)
                setBody(UpdateDateOfBirthRequest(dateOfBirth = "1990-05-15"))
            }

            val meResponse = client.get("/auth/me")
            assertEquals(HttpStatusCode.OK, meResponse.status)
            val profile = meResponse.body<UserProfileResponse>()
            assertEquals("1990-05-15", profile.dateOfBirth)
        }

    // ── 3. Popular searches endpoint ──

    @Test
    fun `GET popular services should return 200`() =
        integrationTestApplication {
            val client = createTestClient()

            val response = client.get("/search/services/popular?cityName=Batatais")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.body<PopularServicesResponse>()
            assertNotNull(body)
        }

    @Test
    fun `GET popular services with no events should return empty list`() =
        integrationTestApplication {
            val client = createTestClient()

            val response = client.get("/search/services/popular?cityName=CidadeInexistente")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.body<PopularServicesResponse>()
            assertTrue(body.services.isEmpty())
            assertFalse(body.isLocalResults)
        }

    // ── 4. Professional profile includes phone from user account ──

    @Test
    fun `GET professional profile should include phone from user account`() =
        integrationTestApplication {
            val phone = "+5511999990040"
            val token = obtainAuthToken(phone)
            completeNameStep(token, "Maria da Silva")
            setUserPhoto(token, "https://example.com/photo.jpg")

            // Set date of birth (required for profile confirmation)
            val client = createTestClient(token)
            client.put("/auth/me/date-of-birth") {
                contentType(ContentType.Application.Json)
                setBody(UpdateDateOfBirthRequest(dateOfBirth = "1990-05-15"))
            }

            createAndConfirmProfile(token)

            // Get the profile via the authenticated endpoint
            val meResponse = client.get("/professional-profile/me")
            assertEquals(HttpStatusCode.OK, meResponse.status)

            val profileBody = meResponse.bodyAsText()
            assertTrue(profileBody.contains("\"phone\""))
            assertTrue(profileBody.contains(phone))
        }

    // ── 5. 18+ enforcement on profile confirmation ──

    @Test
    fun `profile confirmation without date of birth should fail`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990050")
            completeNameStep(token, "Maria da Silva")
            setUserPhoto(token, "https://example.com/photo.jpg")
            val client = createTestClient(token)

            // Create draft first
            val draftResponse =
                client.post("/professional-profile/draft") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"inputText":"Pintor residencial em São Paulo","inputMode":"TEXT"}""")
                }
            assertEquals(HttpStatusCode.OK, draftResponse.status)

            // Try to confirm without setting date of birth
            val confirmResponse =
                client.post("/professional-profile/confirm") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"description":"Pintor residencial","selectedServiceIds":[],"cityName":"São Paulo","portfolioPhotoUrls":[]}""",
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, confirmResponse.status)
            val body = confirmResponse.bodyAsText()
            assertTrue(body.contains("DATE_OF_BIRTH_REQUIRED"))
        }

    @Test
    fun `profile confirmation with 18+ user should succeed`() =
        integrationTestApplication {
            val token = obtainAuthToken("+5511999990051")
            completeNameStep(token, "Maria da Silva")
            setUserPhoto(token, "https://example.com/photo.jpg")
            val client = createTestClient(token)

            // Set date of birth first
            client.put("/auth/me/date-of-birth") {
                contentType(ContentType.Application.Json)
                setBody(UpdateDateOfBirthRequest(dateOfBirth = "1990-05-15"))
            }

            createAndConfirmProfile(token)

            // Verify profile was created
            val meResponse = client.get("/professional-profile/me")
            assertEquals(HttpStatusCode.OK, meResponse.status)
        }
}
