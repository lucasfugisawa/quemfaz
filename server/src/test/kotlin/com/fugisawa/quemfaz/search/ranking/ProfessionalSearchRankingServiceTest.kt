package com.fugisawa.quemfaz.search.ranking

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfessionalSearchRankingServiceTest {
    private val rankingService = ProfessionalSearchRankingService()

    private fun createProfile(
        id: String,
        services: List<ProfessionalProfileService>,
        completeness: ProfileCompleteness = ProfileCompleteness.INCOMPLETE,
        lastActiveAt: Instant = Instant.now(),
        viewCount: Int = 0,
        contactClickCount: Int = 0,
    ) = ProfessionalProfile(
        id = ProfessionalProfileId(id),
        userId = UserId("user-$id"),
        knownName = null,
        description = "Description $id",
        normalizedDescription = "Description $id",
        cityId = "batatais",
        services = services,
        portfolioPhotos = emptyList(),
        completeness = completeness,
        status = ProfessionalProfileStatus.PUBLISHED,
        lastActiveAt = lastActiveAt,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        viewCount = viewCount,
        contactClickCount = contactClickCount,
    )

    @Test
    fun `should rank primary matches higher than related ones`() {
        val p1 = createProfile("p1", listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)))
        val p2 = createProfile("p2", listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.RELATED)))

        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "batatais", emptyList())

        val ranked = rankingService.rank(listOf(p2, p1), query)

        assertEquals("p1", ranked[0].id.value)
        assertEquals("p2", ranked[1].id.value)
    }

    @Test
    fun `should apply completeness and recently active bonus`() {
        val p1 =
            createProfile(
                "p1",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                completeness = ProfileCompleteness.COMPLETE,
            )
        val p2 =
            createProfile(
                "p2",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                completeness = ProfileCompleteness.INCOMPLETE,
            )

        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "batatais", emptyList())

        val ranked = rankingService.rank(listOf(p2, p1), query)

        assertEquals("p1", ranked[0].id.value)
        assertEquals("p2", ranked[1].id.value)
    }

    @Test
    fun `profile with contact clicks ranks higher than one without`() {
        val profileWithClicks =
            createProfile(
                "with-clicks",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                contactClickCount = 10,
            )
        val profileWithout =
            createProfile(
                "without-clicks",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                contactClickCount = 0,
            )
        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "batatais", emptyList())
        val ranked = rankingService.rank(listOf(profileWithout, profileWithClicks), query)
        assertTrue(ranked.first().id.value == "with-clicks")
    }

    @Test
    fun `profile with views ranks higher than one without`() {
        val profileWithViews =
            createProfile(
                "with-views",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                viewCount = 30,
            )
        val profileWithout =
            createProfile(
                "without-views",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                viewCount = 0,
            )
        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "batatais", emptyList())
        val ranked = rankingService.rank(listOf(profileWithout, profileWithViews), query)
        assertTrue(ranked.first().id.value == "with-views")
    }

    @Test
    fun `contact click score is capped at 20 clicks`() {
        val profileAtCap =
            createProfile(
                "at-cap",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                contactClickCount = 20,
            )
        val profileOverCap =
            createProfile(
                "over-cap",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                contactClickCount = 100,
            )
        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "batatais", emptyList())
        val rankedOverFirst = rankingService.rank(listOf(profileOverCap, profileAtCap), query)
        val rankedAtFirst = rankingService.rank(listOf(profileAtCap, profileOverCap), query)
        // With stable sort and equal scores, input order is preserved both ways
        assertTrue(rankedOverFirst[0].id.value == "over-cap")
        assertTrue(rankedAtFirst[0].id.value == "at-cap")
    }

    @Test
    fun `view score is capped at 50 views`() {
        val profileAtCap =
            createProfile(
                "at-cap",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                viewCount = 50,
            )
        val profileOverCap =
            createProfile(
                "over-cap",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                viewCount = 200,
            )
        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "batatais", emptyList())
        val rankedOverFirst = rankingService.rank(listOf(profileOverCap, profileAtCap), query)
        val rankedAtFirst = rankingService.rank(listOf(profileAtCap, profileOverCap), query)
        assertTrue(rankedOverFirst[0].id.value == "over-cap")
        assertTrue(rankedAtFirst[0].id.value == "at-cap")
    }
}
