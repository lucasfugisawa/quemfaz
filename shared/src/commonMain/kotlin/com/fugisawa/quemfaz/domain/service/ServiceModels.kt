package com.fugisawa.quemfaz.domain.service

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import kotlinx.serialization.Serializable

@Serializable
enum class ServiceCategory {
    CLEANING,
    REPAIRS,
    PAINTING,
    GARDEN,
    EVENTS,
    BEAUTY,
    MOVING_AND_ASSEMBLY,
    OTHER
}

@Serializable
enum class ServiceMatchLevel {
    PRIMARY,
    SECONDARY,
    RELATED
}

@Serializable
data class CanonicalService(
    val id: CanonicalServiceId,
    val displayName: String,
    val description: String,
    val category: ServiceCategory,
    val baseAliases: List<String>
)

@Serializable
data class ProfessionalService(
    val serviceId: CanonicalServiceId,
    val matchLevel: ServiceMatchLevel
)
