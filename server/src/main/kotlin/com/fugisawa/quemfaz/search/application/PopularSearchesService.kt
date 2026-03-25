package com.fugisawa.quemfaz.search.application

import com.fugisawa.quemfaz.contract.search.PopularServiceDto
import com.fugisawa.quemfaz.contract.search.PopularServicesResponse
import com.fugisawa.quemfaz.search.domain.SearchEventRepository
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class PopularSearchesService(
    private val searchEventRepository: SearchEventRepository,
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

    fun getPopularServices(cityName: String?): PopularServicesResponse {
        val cacheKey = cityName ?: "__global__"
        val cached = cache[cacheKey]
        if (cached != null && Duration.between(cached.cachedAt, Instant.now()).toMinutes() < cacheDurationMinutes) {
            return cached.response
        }
        val response = computePopularServices(cityName)
        cache[cacheKey] = CacheEntry(response, Instant.now())
        return response
    }

    private fun computePopularServices(cityName: String?): PopularServicesResponse {
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
