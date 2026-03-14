package com.fugisawa.quemfaz.search.ranking

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class ProfessionalSearchRankingServiceTest {
    private val rankingService = ProfessionalSearchRankingService()

    private fun createProfile(
        id: String,
        services: List<ProfessionalProfileService>,
        neighborhoods: List<String> = emptyList(),
        completeness: ProfileCompleteness = ProfileCompleteness.INCOMPLETE,
        lastActiveAt: Instant = Instant.now(),
    ) = ProfessionalProfile(
        id = ProfessionalProfileId(id),
        userId = UserId("user-$id"),
        knownName = null,
        description = "Description $id",
        normalizedDescription = "Description $id",
        contactPhone = "123456",
        whatsappPhone = "123456",
        cityName = "Batatais",
        neighborhoods = neighborhoods,
        services = services,
        portfolioPhotos = emptyList(),
        completeness = completeness,
        status = ProfessionalProfileStatus.PUBLISHED,
        lastActiveAt = lastActiveAt,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `should rank primary matches higher than related ones`() {
        val p1 = createProfile("p1", listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)))
        val p2 = createProfile("p2", listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.RELATED)))

        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "Batatais", emptyList(), emptyList())

        val ranked = rankingService.rank(listOf(p2, p1), query)

        assertEquals("p1", ranked[0].id.value)
        assertEquals("p2", ranked[1].id.value)
    }

    @Test
    fun `should apply neighborhood bonus`() {
        val p1 =
            createProfile(
                "p1",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                neighborhoods = listOf("Centro"),
            )
        val p2 =
            createProfile(
                "p2",
                listOf(ProfessionalProfileService("clean-house", ServiceMatchLevel.PRIMARY)),
                neighborhoods = listOf("Other"),
            )

        val query =
            InterpretedSearchQuery(
                "limpeza no centro",
                "limpeza no centro",
                listOf("clean-house"),
                "Batatais",
                listOf("Centro"),
                emptyList(),
            )

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

        val query = InterpretedSearchQuery("limpeza", "limpeza", listOf("clean-house"), "Batatais", emptyList(), emptyList())

        val ranked = rankingService.rank(listOf(p2, p1), query)

        assertEquals("p1", ranked[0].id.value)
        assertEquals("p2", ranked[1].id.value)
    }
}
