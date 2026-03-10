package com.fugisawa.quemfaz.search.ranking

import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProfessionalSearchRankingService {
    companion object {
        const val PRIMARY_MATCH_POINTS = 100
        const val SECONDARY_MATCH_POINTS = 50
        const val RELATED_MATCH_POINTS = 20

        const val NEIGHBORHOOD_BONUS = 30
        const val COMPLETENESS_BONUS = 15
        const val RECENTLY_ACTIVE_BONUS = 10

        val RECENTLY_ACTIVE_THRESHOLD = 7L // days
    }

    fun rank(
        profiles: List<ProfessionalProfile>,
        query: InterpretedSearchQuery,
    ): List<ProfessionalProfile> =
        profiles
            .associateWith { calculateScore(it, query) }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

    private fun calculateScore(
        profile: ProfessionalProfile,
        query: InterpretedSearchQuery,
    ): Int {
        var score = 0

        // 1. Service Matching
        query.serviceIds.forEach { queryServiceId ->
            val profileService = profile.services.find { it.serviceId == queryServiceId }
            if (profileService != null) {
                score +=
                    when (profileService.matchLevel) {
                        ServiceMatchLevel.PRIMARY -> PRIMARY_MATCH_POINTS
                        ServiceMatchLevel.SECONDARY -> SECONDARY_MATCH_POINTS
                        ServiceMatchLevel.RELATED -> RELATED_MATCH_POINTS
                    }
            }
        }

        // 2. Neighborhood Bonus
        if (query.neighborhoods.isNotEmpty()) {
            val hasNeighborhoodMatch =
                profile.neighborhoods.any { profileNeighborhood ->
                    query.neighborhoods.any { queryNeighborhood ->
                        profileNeighborhood.equals(queryNeighborhood, ignoreCase = true)
                    }
                }
            if (hasNeighborhoodMatch) {
                score += NEIGHBORHOOD_BONUS
            }
        }

        // 3. Completeness Bonus
        if (profile.completeness == ProfileCompleteness.COMPLETE) {
            score += COMPLETENESS_BONUS
        }

        // 4. Recently Active Bonus
        val now = Instant.now()
        val daysSinceActive = ChronoUnit.DAYS.between(profile.lastActiveAt, now)
        if (daysSinceActive <= RECENTLY_ACTIVE_THRESHOLD) {
            score += RECENTLY_ACTIVE_BONUS
        }

        return score
    }
}
