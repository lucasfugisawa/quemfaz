package com.fugisawa.quemfaz.domain.city

import com.fugisawa.quemfaz.core.id.CityId
import kotlinx.serialization.Serializable

/**
 * Represents a city where services are offered.
 *
 * @property id Unique identifier (slug, e.g. "ribeirao-preto")
 * @property name Display name (e.g., "Ribeirão Preto")
 * @property stateCode State code (e.g., "SP")
 * @property country Country code — defaults to "BR" (Brazil only for MVP)
 * @property latitude City center latitude, for future location-to-city mapping
 * @property longitude City center longitude, for future location-to-city mapping
 * @property isActive Whether this city is currently supported by the app
 */
@Serializable
data class City(
    val id: CityId,
    val name: String,
    val stateCode: String,
    val country: String = "BR",
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean,
)

/**
 * Indicates how a user's city was determined.
 *
 * @property AUTO_DETECTED City was automatically detected (e.g., via GPS or IP)
 * @property MANUALLY_SELECTED User explicitly chose this city
 */
@Serializable
enum class CitySelectionMode {
    AUTO_DETECTED,
    MANUALLY_SELECTED,
}

/**
 * Captures the user's current city context for personalized results.
 *
 * @property city The active city
 * @property selectionMode How the city was determined
 */
@Serializable
data class UserCityContext(
    val city: City,
    val selectionMode: CitySelectionMode,
)
