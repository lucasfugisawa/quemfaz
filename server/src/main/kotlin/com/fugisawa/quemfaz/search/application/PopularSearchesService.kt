package com.fugisawa.quemfaz.search.application

import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.search.PopularServiceDto
import com.fugisawa.quemfaz.contract.search.PopularServicesResponse
import com.fugisawa.quemfaz.search.domain.SearchEventRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class PopularSearchesService(
    private val searchEventRepository: SearchEventRepository,
    private val cityService: CityService,
    private val minSearchesThreshold: Int = 10,
    private val windowDays: Int = 30,
    private val limit: Int = 8,
    private val cacheDurationMinutes: Long = 15,
) {
    private data class CacheEntry(
        val response: PopularServicesResponse,
        val cachedAt: Instant,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun getPopularServices(cityId: String?): PopularServicesResponse {
        val cacheKey = cityId ?: "__global__"
        val cached = cache[cacheKey]
        if (cached != null && Duration.between(cached.cachedAt, Instant.now()).toMinutes() < cacheDurationMinutes) {
            return cached.response
        }
        val response = computePopularServices(cityId)
        cache[cacheKey] = CacheEntry(response, Instant.now())
        return response
    }

    private fun computePopularServices(cityId: String?): PopularServicesResponse {
        val cityName = cityService.resolveNameFromId(cityId)
        if (cityName != null) {
            val cityCount = searchEventRepository.countSearchesInWindow(cityName, windowDays)
            if (cityCount >= minSearchesThreshold) {
                val results = searchEventRepository.getPopularServices(cityName, limit, windowDays)
                return PopularServicesResponse(
                    services = results.map { PopularServiceDto(it.serviceId, it.displayName) },
                    isLocalResults = true,
                )
            }
        }
        val results = searchEventRepository.getPopularServices(null, limit, windowDays)
        return PopularServicesResponse(
            services = results.map { PopularServiceDto(it.serviceId, it.displayName) },
            isLocalResults = false,
        )
    }
}
