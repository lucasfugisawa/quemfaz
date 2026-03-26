package com.fugisawa.quemfaz.engagement.application

import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.contract.engagement.TrackContactClickRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.engagement.domain.ContactChannel
import com.fugisawa.quemfaz.engagement.domain.ContactClickEvent
import com.fugisawa.quemfaz.engagement.domain.ContactClickEventRepository
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class TrackContactClickService(
    private val contactClickEventRepository: ContactClickEventRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val cityService: CityService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId?,
        request: TrackContactClickRequest,
    ) {
        val profileId = ProfessionalProfileId(request.professionalProfileId)
        val profile =
            profileRepository.findById(profileId)
                ?: throw IllegalArgumentException("Profile not found")

        if (profile.status != ProfessionalProfileStatus.PUBLISHED) {
            throw IllegalStateException("Cannot track contact for non-public profile")
        }

        val event =
            ContactClickEvent(
                id = UUID.randomUUID().toString(),
                professionalProfileId = profileId,
                userId = userId,
                channel =
                    when (request.channel) {
                        ContactChannelDto.WHATSAPP -> ContactChannel.WHATSAPP
                        ContactChannelDto.PHONE_CALL -> ContactChannel.PHONE_CALL
                    },
                cityName = cityService.resolveNameFromId(profile.cityId),
                source = request.source,
                createdAt = Instant.now(),
            )

        transaction {
            contactClickEventRepository.save(event)
            profileRepository.incrementContactClickCount(profileId)
            profileRepository.updateLastActiveAt(profileId)
        }
        logger.info("Contact click tracked for profile ${profileId.value} via ${event.channel}")
    }
}
