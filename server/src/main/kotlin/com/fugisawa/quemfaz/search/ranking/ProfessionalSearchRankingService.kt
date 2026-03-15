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
        const val CITY_MISMATCH_PENALTY = -50

        val RECENTLY_ACTIVE_THRESHOLD = 7L // days

        const val CONTACT_CLICK_CAP = 20
        const val CONTACT_CLICK_POINTS_PER = 2
        const val VIEW_CAP = 50
        const val VIEW_POINTS_PER = 0.5
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

        // 2. Completeness Bonus
        if (profile.completeness == ProfileCompleteness.COMPLETE) {
            score += COMPLETENESS_BONUS
        }

        // 4. Recently Active Bonus
        val now = Instant.now()
        val daysSinceActive = ChronoUnit.DAYS.between(profile.lastActiveAt, now)
        if (daysSinceActive <= RECENTLY_ACTIVE_THRESHOLD) {
            score += RECENTLY_ACTIVE_BONUS
        }

        // 5. Engagement: Contact Clicks
        score += (minOf(profile.contactClickCount, CONTACT_CLICK_CAP) * CONTACT_CLICK_POINTS_PER)

        // 6. Engagement: Profile Views
        score += (minOf(profile.viewCount, VIEW_CAP) * VIEW_POINTS_PER).toInt()

        // 7. City Match
        if (query.cityName != null) {
            if (profile.cityName != null) {
                if (!profile.cityName.equals(query.cityName, ignoreCase = true)) {
                    score += CITY_MISMATCH_PENALTY
                }
            } else {
                // Profile has no city, but query has one.
                score += CITY_MISMATCH_PENALTY / 2
            }
        } else {
            // No city context in query.
            // If profile has a city, it's just a regular result.
            // We could optionally give a small bonus to profiles without city (global/remote services)
            // but for now, we do nothing (neutral score).
        }

        return score
    }
}
