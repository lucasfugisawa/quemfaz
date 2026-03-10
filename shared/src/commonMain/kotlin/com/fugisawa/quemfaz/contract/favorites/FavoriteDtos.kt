package com.fugisawa.quemfaz.contract.favorites

import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import kotlinx.serialization.Serializable

@Serializable
data class FavoritesListResponse(
    val favorites: List<ProfessionalProfileResponse>
)
