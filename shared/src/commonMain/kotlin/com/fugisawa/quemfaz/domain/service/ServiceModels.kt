package com.fugisawa.quemfaz.domain.service

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import kotlinx.serialization.Serializable

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
