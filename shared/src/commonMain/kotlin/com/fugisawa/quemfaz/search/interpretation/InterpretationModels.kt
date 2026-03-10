package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import kotlinx.serialization.Serializable

@Serializable
data class InterpretedCanonicalService(
    val serviceId: CanonicalServiceId,
    val matchLevel: ServiceMatchLevel
)

@Serializable
data class InterpretedSearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val canonicalServices: List<InterpretedCanonicalService>,
    val cityName: String?,
    val neighborhoods: List<String>,
    val freeTextAliases: List<String>
)

@Serializable
data class InterpretedProfessionalInput(
    val originalInput: String,
    val normalizedDescription: String,
    val canonicalServices: List<InterpretedCanonicalService>,
    val cityName: String?,
    val neighborhoods: List<String>,
    val freeTextAliases: List<String>,
    val missingFields: List<String>,
    val followUpQuestions: List<String>
)
