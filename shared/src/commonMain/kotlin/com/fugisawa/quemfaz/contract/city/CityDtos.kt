package com.fugisawa.quemfaz.contract.city

import kotlinx.serialization.Serializable

@Serializable
data class CityResponse(
    val id: String,
    val name: String,
    val stateCode: String,
)

@Serializable
data class CitiesResponse(
    val cities: List<CityResponse>,
)
