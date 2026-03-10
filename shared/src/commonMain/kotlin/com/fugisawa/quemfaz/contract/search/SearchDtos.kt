package com.fugisawa.quemfaz.contract.search

import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import kotlinx.serialization.Serializable

@Serializable
data class SearchProfessionalsRequest(
    val query: String,
    val cityName: String?,
    val inputMode: InputMode
)

@Serializable
data class SearchProfessionalsResponse(
    val normalizedQuery: String,
    val interpretedServices: List<InterpretedServiceDto>,
    val results: List<ProfessionalProfileResponse>
)
