package com.fugisawa.quemfaz.city.application

import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.contract.city.CitiesResponse
import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.domain.city.City

class CityService(
    private val cityRepository: CityRepository,
) {
    fun listActive(): CitiesResponse {
        val cities = cityRepository.listActive()
        return CitiesResponse(
            cities = cities.map { it.toResponse() },
        )
    }

    fun findById(id: String): City? = cityRepository.findById(id)

    fun resolveNameFromId(cityId: String?): String? {
        if (cityId == null) return null
        return cityRepository.findById(cityId)?.name
    }

    fun resolveIdFromName(cityName: String?): String? {
        if (cityName == null) return null
        return cityRepository.findByName(cityName)?.id?.value
    }

    private fun City.toResponse(): CityResponse =
        CityResponse(
            id = id.value,
            name = name,
            stateCode = stateCode,
        )
}
