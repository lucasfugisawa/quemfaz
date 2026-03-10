package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.contract.profile.*
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.*
import com.fugisawa.quemfaz.profile.interpretation.ProfessionalInputInterpreter
import com.fugisawa.quemfaz.auth.domain.UserRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class CreateProfessionalProfileDraftService(
    private val interpreter: ProfessionalInputInterpreter
) {
    fun execute(userId: UserId, request: CreateProfessionalProfileDraftRequest): CreateProfessionalProfileDraftResponse {
        return interpreter.interpret(request.inputText, request.inputMode)
    }
}

class ConfirmProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(userId: UserId, request: ConfirmProfessionalProfileRequest): ProfessionalProfileResponse {
        val user = userRepository.findById(userId) ?: throw IllegalStateException("User not found")
        
        val existingProfile = profileRepository.findByUserId(userId)
        
        val profileId = existingProfile?.id ?: ProfessionalProfileId(UUID.randomUUID().toString())
        
        val services = request.selectedServiceIds.map { serviceId ->
            ProfessionalProfileService(serviceId, ServiceMatchLevel.PRIMARY)
        }
        
        val portfolioPhotos = request.portfolioPhotoUrls.map { url ->
            PortfolioPhoto(UUID.randomUUID().toString(), url, null, Instant.now())
        }
        
        val completeness = if (
            request.normalizedDescription.isNotBlank() &&
            request.selectedServiceIds.isNotEmpty() &&
            !request.cityName.isNullOrBlank() &&
            request.contactPhone.isNotBlank()
        ) {
            ProfileCompleteness.COMPLETE
        } else {
            ProfileCompleteness.INCOMPLETE
        }
        
        val profile = ProfessionalProfile(
            id = profileId,
            userId = userId,
            description = request.normalizedDescription,
            normalizedDescription = request.normalizedDescription,
            contactPhone = request.contactPhone,
            whatsappPhone = request.whatsAppPhone,
            cityName = request.cityName,
            neighborhoods = request.neighborhoods,
            services = services,
            portfolioPhotos = portfolioPhotos,
            completeness = completeness,
            status = ProfessionalProfileStatus.PUBLISHED,
            lastActiveAt = Instant.now(),
            createdAt = existingProfile?.createdAt ?: Instant.now(),
            updatedAt = Instant.now()
        )
        
        val savedProfile = profileRepository.save(profile)
        logger.info("Professional profile ${savedProfile.id.value} published for user ${userId.value}")
        
        return mapToResponse(savedProfile, user.name, user.photoUrl)
    }
}

class GetMyProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository
) {
    fun execute(userId: UserId): ProfessionalProfileResponse? {
        val profile = profileRepository.findByUserId(userId) ?: return null
        val user = userRepository.findById(userId)
        return mapToResponse(profile, user?.name, user?.photoUrl)
    }
}

class GetPublicProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository
) {
    fun execute(profileId: ProfessionalProfileId): ProfessionalProfileResponse? {
        val profile = profileRepository.findById(profileId) ?: return null
        if (profile.status != ProfessionalProfileStatus.PUBLISHED) return null
        
        val user = userRepository.findById(profile.userId)
        return mapToResponse(profile, user?.name, user?.photoUrl)
    }
}

private fun mapToResponse(profile: ProfessionalProfile, userName: String?, userPhotoUrl: String?): ProfessionalProfileResponse {
    return ProfessionalProfileResponse(
        id = profile.id.value,
        name = userName,
        photoUrl = userPhotoUrl ?: profile.portfolioPhotos.firstOrNull()?.photoUrl,
        description = profile.normalizedDescription ?: "",
        cityName = profile.cityName ?: "",
        neighborhoods = profile.neighborhoods,
        services = profile.services.map { svc ->
            val canonical = CanonicalServices.findById(com.fugisawa.quemfaz.core.id.CanonicalServiceId(svc.serviceId))
            InterpretedServiceDto(svc.serviceId, canonical?.displayName ?: svc.serviceId, svc.matchLevel.name)
        },
        profileComplete = profile.completeness == ProfileCompleteness.COMPLETE,
        activeRecently = profile.lastActiveAt.isAfter(Instant.now().minusSeconds(86400 * 7)), // Active in last 7 days
        whatsAppPhone = profile.whatsappPhone,
        contactPhone = profile.contactPhone ?: ""
    )
}
