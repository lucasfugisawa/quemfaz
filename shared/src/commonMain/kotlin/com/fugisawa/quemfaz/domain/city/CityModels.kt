package com.fugisawa.quemfaz.domain.city

import com.fugisawa.quemfaz.core.id.CityId
import kotlinx.serialization.Serializable

@Serializable
data class City(
    val id: CityId,
    val name: String,
    val state: String,
    val country: String = "BR",
    val isActive: Boolean
)

@Serializable
enum class CitySelectionMode {
    AUTO_DETECTED,
    MANUALLY_SELECTED
}

@Serializable
data class UserCityContext(
    val city: City,
    val selectionMode: CitySelectionMode
)
