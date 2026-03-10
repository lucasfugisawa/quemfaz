package com.fugisawa.quemfaz.search.application

import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.*
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import com.fugisawa.quemfaz.search.domain.SearchQuery
import com.fugisawa.quemfaz.search.domain.SearchQueryRepository
import com.fugisawa.quemfaz.search.interpretation.SearchQueryInterpreter
import com.fugisawa.quemfaz.search.ranking.ProfessionalSearchRankingService
import org.junit.Test
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

    private class FakeProfileRepository(val profiles: List<ProfessionalProfile>) : ProfessionalProfileRepository {
        override fun findByUserId(userId: UserId): ProfessionalProfile? = null
        override fun findById(id: ProfessionalProfileId): ProfessionalProfile? = null
        override fun save(profile: ProfessionalProfile): ProfessionalProfile = profile
        override fun listPublishedByCity(cityName: String): List<ProfessionalProfile> = profiles.filter { it.cityName == cityName && it.status == ProfessionalProfileStatus.PUBLISHED }
        override fun updateStatus(id: ProfessionalProfileId, status: ProfessionalProfileStatus): Boolean = false
    }

    private class FakeUserRepository : UserRepository {
        override fun create(user: User): User = user
        override fun findById(id: UserId): User? = User(id, "User ${id.value}", null, UserStatus.ACTIVE, Instant.now(), Instant.now())
        override fun updateProfile(id: UserId, name: String, photoUrl: String?): User? = null
        override fun updateStatus(id: UserId, status: UserStatus): Boolean = false
    }

    private class FakeInterpreter : SearchQueryInterpreter {
        override fun interpret(query: String, cityContext: String?): InterpretedSearchQuery {
            return InterpretedSearchQuery(query, query.lowercase(), listOf("clean-house"), cityContext ?: "Batatais", emptyList(), emptyList())
        }
    }

    @Test
    fun `should perform search and persist query`() {
        val profile = ProfessionalProfile(
            id = ProfessionalProfileId("p1"),
            userId = UserId("u1"),
            description = "Desc",
            normalizedDescription = "Desc",
            contactPhone = "123",
            whatsappPhone = "123",
            cityName = "Batatais",
            neighborhoods = emptyList(),
            services = listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
            portfolioPhotos = emptyList(),
            completeness = ProfileCompleteness.COMPLETE,
            status = ProfessionalProfileStatus.PUBLISHED,
            lastActiveAt = Instant.now(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val searchQueryRepo = FakeSearchQueryRepository()
        val service = SearchProfessionalsService(
            interpreter = FakeInterpreter(),
            rankingService = ProfessionalSearchRankingService(),
            searchQueryRepository = searchQueryRepo,
            profileRepository = FakeProfileRepository(listOf(profile)),
            userRepository = FakeUserRepository()
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
