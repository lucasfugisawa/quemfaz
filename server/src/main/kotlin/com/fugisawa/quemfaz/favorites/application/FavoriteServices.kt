package com.fugisawa.quemfaz.favorites.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.favorites.FavoritesListResponse
import com.fugisawa.quemfaz.core.id.FavoriteId
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.favorites.domain.Favorite
import com.fugisawa.quemfaz.favorites.domain.FavoriteRepository
import com.fugisawa.quemfaz.profile.application.ProfileResponseMapper
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class AddFavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val profileRepository: ProfessionalProfileRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId,
        profileId: ProfessionalProfileId,
    ) {
        val profile =
            profileRepository.findById(profileId)
                ?: throw IllegalArgumentException("Profile not found")

        if (profile.status != ProfessionalProfileStatus.PUBLISHED) {
            throw IllegalStateException("Only published profiles can be favorited")
        }

        if (favoriteRepository.exists(userId, profileId)) {
            logger.info("User ${userId.value} already favorited profile ${profileId.value}")
            return
        }

        val favorite =
            Favorite(
                id = FavoriteId(UUID.randomUUID().toString()),
                userId = userId,
                professionalProfileId = profileId,
                createdAt = Instant.now(),
            )

        favoriteRepository.add(favorite)
        logger.info("User ${userId.value} favorited profile ${profileId.value}")
    }
}

class RemoveFavoriteService(
    private val favoriteRepository: FavoriteRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId,
        profileId: ProfessionalProfileId,
    ) {
        favoriteRepository.remove(userId, profileId)
        logger.info("User ${userId.value} removed favorite for profile ${profileId.value}")
    }
}

class ListFavoritesService(
    private val favoriteRepository: FavoriteRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(userId: UserId): FavoritesListResponse {
        val favorites = favoriteRepository.listByUserId(userId)
        val profiles =
            favorites.mapNotNull { fav ->
                val profile = profileRepository.findById(fav.professionalProfileId)
                if (profile != null && profile.status == ProfessionalProfileStatus.PUBLISHED) {
                    val user = userRepository.findById(profile.userId)
                    val phone = phoneAuthRepository.findByUserId(profile.userId)?.phoneNumber ?: ""
                    profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone)
                } else {
                    null
                }
            }
        return FavoritesListResponse(profiles)
    }
}
