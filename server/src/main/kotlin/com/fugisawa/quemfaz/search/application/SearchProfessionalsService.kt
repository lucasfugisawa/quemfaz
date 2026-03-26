package com.fugisawa.quemfaz.search.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.application.ProfileResponseMapper
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import com.fugisawa.quemfaz.search.domain.SearchEvent
import com.fugisawa.quemfaz.search.domain.SearchEventRepository
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
    private val searchEventRepository: SearchEventRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val catalogService: CatalogService,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId?,
        request: SearchProfessionalsRequest,
    ): SearchProfessionalsResponse {
        // 1. Interpret query — interpreter works with city names (free text extraction)
        val requestCityName = cityService.resolveNameFromId(request.cityId)
        val interpreted = interpreter.interpret(request.query, requestCityName)

        // Resolve city: interpreted city name → cityId, fallback to request.cityId
        val resolvedCityId = cityService.resolveIdFromName(interpreted.cityId) ?: request.cityId
        val resolvedCityName = cityService.resolveNameFromId(resolvedCityId) ?: interpreted.cityId

        // 2. Persist query for analytics (uses city name for analytics tables)
        val searchQuery =
            SearchQuery(
                id = UUID.randomUUID().toString(),
                userId = userId,
                originalQuery = request.query,
                normalizedQuery = interpreted.normalizedQuery,
                cityName = resolvedCityName,
                interpretedServiceIds = interpreted.serviceIds,
                inputMode = request.inputMode,
                createdAt = Instant.now(),
            )
        searchQueryRepository.create(searchQuery)

        // 2b. Log search events for popular-searches analytics
        if (interpreted.serviceIds.isNotEmpty() && resolvedCityName != null) {
            val now = Instant.now()
            val events =
                interpreted.serviceIds.map { serviceId ->
                    SearchEvent(
                        id = UUID.randomUUID().toString(),
                        resolvedServiceId = serviceId,
                        cityName = resolvedCityName,
                        createdAt = now,
                    )
                }
            try {
                searchEventRepository.logEvents(events)
            } catch (e: Exception) {
                logger.warn("Failed to log search events", e)
            }
        }

        // 3. Retrieve candidate profiles
        val candidates = profileRepository.search(interpreted.serviceIds, resolvedCityId)

        // 4. Rank profiles
        val ranked =
            rankingService.rank(
                candidates,
                InterpretedSearchQuery(
                    originalQuery = interpreted.originalQuery,
                    normalizedQuery = interpreted.normalizedQuery,
                    serviceIds = interpreted.serviceIds,
                    cityId = resolvedCityId,
                    freeTextAliases = interpreted.freeTextAliases,
                    llmUnavailable = interpreted.llmUnavailable,
                    blockedDescriptions = interpreted.blockedDescriptions,
                ),
            )

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
                    val canonical = catalogService.findById(serviceId)
                    InterpretedServiceDto(serviceId, canonical?.displayName ?: serviceId, "PRIMARY")
                },
            results =
                pagedResults.map { profile ->
                    val user = userRepository.findById(profile.userId)
                    val phone = phoneAuthRepository.findByUserId(profile.userId)?.phoneNumber ?: ""
                    profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone)
                },
            page = page,
            pageSize = pageSize,
            totalCount = totalCount,
            llmUnavailable = interpreted.llmUnavailable,
            blockedDescriptions = interpreted.blockedDescriptions,
        )
    }
}
