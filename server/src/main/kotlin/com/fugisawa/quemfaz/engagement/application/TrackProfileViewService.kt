package com.fugisawa.quemfaz.engagement.application

import com.fugisawa.quemfaz.contract.engagement.TrackProfileViewRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.engagement.domain.ProfileViewEvent
import com.fugisawa.quemfaz.engagement.domain.ProfileViewEventRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class TrackProfileViewService(
    private val profileViewEventRepository: ProfileViewEventRepository,
    private val profileRepository: ProfessionalProfileRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId?,
        request: TrackProfileViewRequest,
    ) {
        val profileId = ProfessionalProfileId(request.professionalProfileId)
        val profile =
            profileRepository.findById(profileId)
                ?: throw IllegalArgumentException("Profile not found")

        if (profile.status != ProfessionalProfileStatus.PUBLISHED) {
            throw IllegalStateException("Cannot track view for non-public profile")
        }

        val event =
            ProfileViewEvent(
                id = UUID.randomUUID().toString(),
                professionalProfileId = profileId,
                userId = userId,
                cityName = profile.cityName,
                source = request.source,
                createdAt = Instant.now(),
            )

        profileViewEventRepository.save(event)
        logger.info("Profile view tracked for profile ${profileId.value}")
    }
}
