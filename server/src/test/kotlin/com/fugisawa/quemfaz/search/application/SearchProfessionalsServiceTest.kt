package com.fugisawa.quemfaz.search.application

import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentity
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import com.fugisawa.quemfaz.search.domain.SearchQuery
import com.fugisawa.quemfaz.search.domain.SearchQueryRepository
import com.fugisawa.quemfaz.search.interpretation.SearchQueryInterpreter
import com.fugisawa.quemfaz.search.ranking.ProfessionalSearchRankingService
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SearchProfessionalsServiceTest {
    private class FakeSearchQueryRepository : SearchQueryRepository {
        var lastSaved: SearchQuery? = null

        override fun create(searchQuery: SearchQuery): SearchQuery {
            lastSaved = searchQuery
            return searchQuery
        }
    }

    private class FakeProfileRepository(
        val profiles: List<ProfessionalProfile>,
    ) : ProfessionalProfileRepository {
        override fun findByUserId(userId: UserId): ProfessionalProfile? = null

        override fun findById(id: ProfessionalProfileId): ProfessionalProfile? = null

        override fun save(profile: ProfessionalProfile): ProfessionalProfile = profile

        override fun listPublishedByCity(cityName: String): List<ProfessionalProfile> =
            profiles.filter {
                it.cityName == cityName &&
                    it.status == ProfessionalProfileStatus.PUBLISHED
            }

        override fun search(
            serviceIds: List<String>,
            cityName: String?,
        ): List<ProfessionalProfile> =
            profiles.filter { profile ->
                profile.status == ProfessionalProfileStatus.PUBLISHED &&
                    profile.services.any { it.serviceId in serviceIds }
            }

        override fun updateStatus(
            id: ProfessionalProfileId,
            status: ProfessionalProfileStatus,
        ): Boolean = false

        override fun updateKnownName(id: ProfessionalProfileId, knownName: String?): Boolean = false

        override fun incrementViewCount(id: ProfessionalProfileId) {}

        override fun incrementContactClickCount(id: ProfessionalProfileId) {}

        override fun updateLastActiveAt(id: ProfessionalProfileId) {}
    }

    private class FakeUserRepository : UserRepository {
        override fun create(user: User): User = user

        override fun findById(id: UserId): User = User(id, "User ${id.value}", null, UserStatus.ACTIVE, createdAt = Instant.now(), updatedAt = Instant.now())

        override fun updateName(id: UserId, fullName: String): User? = null

        override fun updateDateOfBirth(id: UserId, dateOfBirth: java.time.LocalDate): User? = null

        override fun updatePhotoUrl(id: UserId, photoUrl: String): User? = null

        override fun updateStatus(
            id: UserId,
            status: UserStatus,
        ): Boolean = false
    }

    private class FakePhoneAuthRepository : UserPhoneAuthIdentityRepository {
        override fun findByPhoneNumber(phoneNumber: String): UserPhoneAuthIdentity? = null
        override fun findByUserId(userId: UserId): UserPhoneAuthIdentity? = null
        override fun create(identity: UserPhoneAuthIdentity) = identity
        override fun markVerified(id: String, verifiedAt: Instant): Boolean = false
    }

    private class FakeInterpreter : SearchQueryInterpreter {
        override fun interpret(
            query: String,
            cityContext: String?,
        ): InterpretedSearchQuery =
            InterpretedSearchQuery(query, query.lowercase(), listOf("clean-house"), cityContext ?: "Batatais", emptyList())
    }

    @Test
    fun `should perform search and persist query`() {
        val profile =
            ProfessionalProfile(
                id = ProfessionalProfileId("p1"),
                userId = UserId("u1"),
                knownName = null,
                description = "Desc",
                normalizedDescription = "Desc",
                cityName = "Batatais",
                services = listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                portfolioPhotos = emptyList(),
                completeness = ProfileCompleteness.COMPLETE,
                status = ProfessionalProfileStatus.PUBLISHED,
                lastActiveAt = Instant.now(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val searchQueryRepo = FakeSearchQueryRepository()
        val service =
            SearchProfessionalsService(
                interpreter = FakeInterpreter(),
                rankingService = ProfessionalSearchRankingService(),
                searchQueryRepository = searchQueryRepo,
                profileRepository = FakeProfileRepository(listOf(profile)),
                userRepository = FakeUserRepository(),
                catalogService = mock(),
                phoneAuthRepository = FakePhoneAuthRepository(),
            )

        val request = SearchProfessionalsRequest("faxina", "Batatais", InputMode.TEXT)
        val response = service.execute(UserId("caller"), request)

        assertEquals(1, response.results.size)
        assertEquals("p1", response.results[0].id)
        assertNotNull(searchQueryRepo.lastSaved)
        assertEquals("faxina", searchQueryRepo.lastSaved?.originalQuery)
        assertEquals("caller", searchQueryRepo.lastSaved?.userId?.value)
    }
}
