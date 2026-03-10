package com.fugisawa.quemfaz.favorites.domain

import com.fugisawa.quemfaz.core.id.FavoriteId
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import java.time.Instant

data class Favorite(
    val id: FavoriteId,
    val userId: UserId,
    val professionalProfileId: ProfessionalProfileId,
    val createdAt: Instant
)

interface FavoriteRepository {
    fun add(favorite: Favorite)
    fun remove(userId: UserId, professionalProfileId: ProfessionalProfileId)
    fun exists(userId: UserId, professionalProfileId: ProfessionalProfileId): Boolean
    fun listByUserId(userId: UserId): List<Favorite>
}
