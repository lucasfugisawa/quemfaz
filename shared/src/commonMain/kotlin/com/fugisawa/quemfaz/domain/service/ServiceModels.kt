package com.fugisawa.quemfaz.domain.service

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import kotlinx.serialization.Serializable

/**
 * High-level categories for grouping services.
 * Used for filtering and navigation in the UI.
 */
@Serializable
enum class ServiceCategory(val displayName: String) {
    CLEANING("Limpeza"),
    REPAIRS("Reparos"),
    PAINTING("Pintura"),
    GARDEN("Jardim"),
    EVENTS("Eventos"),
    BEAUTY("Beleza"),
    MOVING_AND_ASSEMBLY("Mudanças e Montagem"),
    OTHER("Outros"),
}

/**
 * Indicates how well a service matches a professional's offering or search query.
 *
 * @property PRIMARY Direct match - the main service offered
 * @property SECONDARY Related service - offered but not the primary focus
 * @property RELATED Tangentially related - might be relevant
 */
@Serializable
enum class ServiceMatchLevel {
    PRIMARY,
    SECONDARY,
    RELATED
}

/**
 * Represents a standardized service type in the platform's taxonomy.
 *
 * Canonical services provide a consistent vocabulary for matching
 * professionals with users searching for services.
 *
 * @property id Unique identifier for this service
 * @property displayName User-facing name (e.g., "Limpeza Residencial")
 * @property description Detailed description of what this service includes
 * @property category High-level grouping for filtering
 * @property baseAliases Common synonyms and alternative names for matching
 */
@Serializable
data class CanonicalService(
    val id: CanonicalServiceId,
    val displayName: String,
    val description: String,
    val category: ServiceCategory,
    val baseAliases: List<String>
)

/**
 * Links a professional profile to a canonical service with a confidence level.
 *
 * @property serviceId Reference to the canonical service
 * @property matchLevel How strongly this service is associated with the professional
 */
@Serializable
data class ProfessionalService(
    val serviceId: CanonicalServiceId,
    val matchLevel: ServiceMatchLevel
)
