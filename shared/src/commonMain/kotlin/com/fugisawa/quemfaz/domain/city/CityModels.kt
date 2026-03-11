package com.fugisawa.quemfaz.domain.city

import com.fugisawa.quemfaz.core.id.CityId
import kotlinx.serialization.Serializable

/**
 * Represents a city where services are offered.
 *
 * @property id Unique identifier for the city
 * @property name City name (e.g., "São Paulo")
 * @property state State code (e.g., "SP")
 * @property country Country code - defaults to "BR" (Brazil only for MVP)
 * @property isActive Whether this city is currently accepting new profiles/services
 */
@Serializable
data class City(
    val id: CityId,
    val name: String,
    val state: String,
    val country: String = "BR", // MVP: Brazil only, will be parameterized for multi-country expansion
    val isActive: Boolean
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
    MANUALLY_SELECTED
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
    val selectionMode: CitySelectionMode
)
