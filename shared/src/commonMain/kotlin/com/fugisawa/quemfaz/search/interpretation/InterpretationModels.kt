package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import kotlinx.serialization.Serializable

/**
 * Represents a canonical service identified from user input,
 * along with the confidence level of the match.
 */
@Serializable
data class InterpretedCanonicalService(
    val serviceId: CanonicalServiceId,
    val matchLevel: ServiceMatchLevel
)

/**
 * Base interface for interpreted user input.
 * Contains common fields extracted from natural language processing.
 */
sealed interface InterpretedInput {
    val originalInput: String
    val normalizedText: String
    val canonicalServices: List<InterpretedCanonicalService>
    val cityName: String?
    val freeTextAliases: List<String>
}

/**
 * Represents a search query that has been interpreted and normalized.
 * Used when users search for professionals.
 *
 * @property originalQuery The raw query string from the user
 * @property normalizedQuery Cleaned and standardized query for matching
 */
@Serializable
data class InterpretedSearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    override val canonicalServices: List<InterpretedCanonicalService>,
    override val cityName: String?,
    override val freeTextAliases: List<String>
) : InterpretedInput {
    override val originalInput: String get() = originalQuery
    override val normalizedText: String get() = normalizedQuery
}

/**
 * Represents professional service description input that has been interpreted.
 * Used when professionals create or update their profiles.
 *
 * @property originalInput The raw description from the professional
 * @property normalizedDescription Cleaned and standardized description
 * @property missingFields Fields that still need to be provided
 * @property followUpQuestions Suggested questions to gather missing information
 */
@Serializable
data class InterpretedProfessionalInput(
    override val originalInput: String,
    val normalizedDescription: String,
    override val canonicalServices: List<InterpretedCanonicalService>,
    override val cityName: String?,
    override val freeTextAliases: List<String>,
    val missingFields: List<String>,
    val followUpQuestions: List<String>
) : InterpretedInput {
    override val normalizedText: String get() = normalizedDescription
}

/**
 * Extension functions for working with interpreted input
 */

/**
 * Returns true if any canonical services were identified.
 */
fun InterpretedInput.hasServices(): Boolean = canonicalServices.isNotEmpty()

/**
 * Returns true if a city was identified.
 */
fun InterpretedInput.hasCity(): Boolean = cityName != null

/**
 * Returns services filtered by match level.
 */
fun InterpretedInput.getServicesByMatchLevel(level: ServiceMatchLevel): List<InterpretedCanonicalService> =
    canonicalServices.filter { it.matchLevel == level }

/**
 * Returns primary match services.
 */
fun InterpretedInput.getPrimaryServices(): List<InterpretedCanonicalService> =
    getServicesByMatchLevel(ServiceMatchLevel.PRIMARY)
