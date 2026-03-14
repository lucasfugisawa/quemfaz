package com.fugisawa.quemfaz.favorites.application

import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.favorites.FavoritesListResponse
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.core.id.FavoriteId
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.favorites.domain.Favorite
import com.fugisawa.quemfaz.favorites.domain.FavoriteRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
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
) {
    fun execute(userId: UserId): FavoritesListResponse {
        val favorites = favoriteRepository.listByUserId(userId)
        val profiles =
            favorites.mapNotNull { fav ->
                val profile = profileRepository.findById(fav.professionalProfileId)
                if (profile != null && profile.status == ProfessionalProfileStatus.PUBLISHED) {
                    val user = userRepository.findById(profile.userId)
                    mapToResponse(profile, user?.firstName ?: "", user?.lastName ?: "", user?.photoUrl)
                } else {
                    null
                }
            }
        return FavoritesListResponse(profiles)
    }

    private fun mapToResponse(
        profile: ProfessionalProfile,
        firstName: String,
        lastName: String,
        userPhotoUrl: String?,
    ): ProfessionalProfileResponse =
        ProfessionalProfileResponse(
            id = profile.id.value,
            firstName = firstName,
            lastName = lastName,
            knownName = profile.knownName,
            photoUrl = userPhotoUrl ?: profile.portfolioPhotos.firstOrNull()?.photoUrl,
            description = profile.normalizedDescription ?: "",
            cityName = profile.cityName ?: "",
            neighborhoods = profile.neighborhoods,
            services =
                profile.services.map { svc ->
                    val canonical =
                        CanonicalServices.findById(
                            com.fugisawa.quemfaz.core.id
                                .CanonicalServiceId(svc.serviceId),
                        )
                    InterpretedServiceDto(svc.serviceId, canonical?.displayName ?: svc.serviceId, svc.matchLevel.name)
                },
            profileComplete = profile.completeness == ProfileCompleteness.COMPLETE,
            activeRecently = profile.lastActiveAt.isAfter(Instant.now().minusSeconds(86400 * 7)),
            whatsAppPhone = profile.whatsappPhone,
            contactPhone = profile.contactPhone ?: "",
            portfolioPhotoUrls = profile.portfolioPhotos.map { it.photoUrl },
        )
}
