package com.fugisawa.quemfaz.search.application

import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.search.domain.SearchQuery
import com.fugisawa.quemfaz.search.domain.SearchQueryRepository
import com.fugisawa.quemfaz.search.interpretation.SearchQueryInterpreter
import com.fugisawa.quemfaz.search.ranking.ProfessionalSearchRankingService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class SearchProfessionalsService(
    private val interpreter: SearchQueryInterpreter,
    private val rankingService: ProfessionalSearchRankingService,
    private val searchQueryRepository: SearchQueryRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId?,
        request: SearchProfessionalsRequest,
    ): SearchProfessionalsResponse {
        // 1. Interpret query
        val interpreted = interpreter.interpret(request.query, request.cityName)
        val city = interpreted.cityName ?: request.cityName // City might be null if not provided and not identified

        // 2. Persist query for analytics
        val searchQuery =
            SearchQuery(
                id = UUID.randomUUID().toString(),
                userId = userId,
                originalQuery = request.query,
                normalizedQuery = interpreted.normalizedQuery,
                cityName = city,
                neighborhoods = interpreted.neighborhoods,
                interpretedServiceIds = interpreted.serviceIds,
                inputMode = request.inputMode,
                createdAt = Instant.now(),
            )
        searchQueryRepository.create(searchQuery)

        // 3. Retrieve candidate profiles
        val candidates = profileRepository.search(interpreted.serviceIds, city)

        // 4. Rank profiles
        val ranked = rankingService.rank(candidates, interpreted)

        // 5. Paginate (in-memory slice — DB still returns all candidates)
        // TODO: move pagination to DB query when result sets grow large
        val page = request.page.coerceAtLeast(0)
        val pageSize = request.pageSize.coerceIn(1, 100)
        val totalCount = ranked.size
        val pagedResults = ranked.drop(page * pageSize).take(pageSize)

        // 6. Map to response
        return SearchProfessionalsResponse(
            normalizedQuery = interpreted.normalizedQuery,
            interpretedServices =
                interpreted.serviceIds.map { serviceId ->
                    val canonical = CanonicalServices.findById(CanonicalServiceId(serviceId))
                    InterpretedServiceDto(serviceId, canonical?.displayName ?: serviceId, "PRIMARY")
                },
            results =
                pagedResults.map { profile ->
                    val user = userRepository.findById(profile.userId)
                    mapToResponse(profile, user?.firstName ?: "", user?.lastName ?: "", user?.photoUrl)
                },
            page = page,
            pageSize = pageSize,
            totalCount = totalCount,
        )
    }

    private fun mapToResponse(
        profile: ProfessionalProfile,
        firstName: String,
        lastName: String,
        userPhotoUrl: String?,
    ): ProfessionalProfileResponse =
        ProfessionalProfileResponse(
            id = profile.id.value,
            firstName = firstName,
            lastName = lastName,
            knownName = profile.knownName,
            photoUrl = userPhotoUrl ?: profile.portfolioPhotos.firstOrNull()?.photoUrl,
            description = profile.normalizedDescription ?: "",
            cityName = profile.cityName ?: "",
            neighborhoods = profile.neighborhoods,
            services =
                profile.services.map { svc ->
                    val canonical = CanonicalServices.findById(CanonicalServiceId(svc.serviceId))
                    InterpretedServiceDto(svc.serviceId, canonical?.displayName ?: svc.serviceId, svc.matchLevel.name)
                },
            profileComplete = profile.completeness == ProfileCompleteness.COMPLETE,
            activeRecently = profile.lastActiveAt.isAfter(Instant.now().minusSeconds(86400 * 7)),
            whatsAppPhone = profile.whatsappPhone,
            contactPhone = profile.contactPhone ?: "",
            portfolioPhotoUrls = profile.portfolioPhotos.map { it.photoUrl },
        )
}
