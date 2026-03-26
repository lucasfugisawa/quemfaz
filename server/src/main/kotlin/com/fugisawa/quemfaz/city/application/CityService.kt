package com.fugisawa.quemfaz.city.application

import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.contract.city.CitiesResponse
import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.domain.city.City
import java.time.Duration
import java.time.Instant

class CityService(
    private val cityRepository: CityRepository,
) {
    private val cacheLock = Any()

    @Volatile
    private var cachedCities: List<City> = emptyList()

    @Volatile
    private var cacheTimestamp: Instant = Instant.EPOCH

    private val cacheTtl: Duration = Duration.ofHours(1)

    private fun ensureCache(): List<City> {
        if (cachedCities.isNotEmpty() && Instant.now() < cacheTimestamp.plus(cacheTtl)) {
            return cachedCities
        }
        synchronized(cacheLock) {
            if (cachedCities.isNotEmpty() && Instant.now() < cacheTimestamp.plus(cacheTtl)) {
                return cachedCities
            }
            cachedCities = cityRepository.listActive()
            cacheTimestamp = Instant.now()
            return cachedCities
        }
    }

    fun listActive(): CitiesResponse =
        CitiesResponse(cities = ensureCache().map { it.toResponse() })

    fun findById(id: String): City? =
        ensureCache().find { it.id.value == id }

    fun resolveNameFromId(cityId: String?): String? {
        if (cityId == null) return null
        return findById(cityId)?.name
    }

    fun resolveIdFromName(cityName: String?): String? {
        if (cityName == null) return null
        return ensureCache().find { it.name.equals(cityName, ignoreCase = true) }?.id?.value
    }

    private fun City.toResponse(): CityResponse =
        CityResponse(id = id.value, name = name, stateCode = stateCode)
}
