package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.profile.ClarifyDraftRequest
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.PortfolioPhoto
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import com.fugisawa.quemfaz.profile.interpretation.ProfessionalInputInterpreter
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class CreateProfessionalProfileDraftService(
    private val interpreter: ProfessionalInputInterpreter,
) {
    fun execute(
        userId: UserId,
        request: CreateProfessionalProfileDraftRequest,
    ): CreateProfessionalProfileDraftResponse = interpreter.interpret(request.inputText, request.inputMode)
}

class ClarifyProfessionalProfileDraftService(
    private val interpreter: ProfessionalInputInterpreter,
) {
    fun execute(
        userId: UserId,
        request: ClarifyDraftRequest,
    ): CreateProfessionalProfileDraftResponse =
        interpreter.interpretWithClarifications(
            request.originalDescription,
            request.clarificationAnswers,
            request.inputMode,
            request.clarificationRound,
        )
}

class ConfirmProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        userId: UserId,
        request: ConfirmProfessionalProfileRequest,
    ): ProfessionalProfileResponse {
        val user = userRepository.findById(userId) ?: throw IllegalStateException("User not found")
        require(user.photoUrl != null) { "Profile photo is required to confirm a professional profile" }
        requireNotNull(user.dateOfBirth) { "DATE_OF_BIRTH_REQUIRED" }
        require(Period.between(user.dateOfBirth, LocalDate.now()).years >= 18) { "UNDERAGE" }

        if (!request.cityId.isNullOrBlank()) {
            require(cityService.findById(request.cityId!!) != null) { "INVALID_CITY_ID" }
        }

        val existingProfile = profileRepository.findByUserId(userId)

        val profileId = existingProfile?.id ?: ProfessionalProfileId(UUID.randomUUID().toString())

        val services =
            request.selectedServiceIds.map { serviceId ->
                ProfessionalProfileService(serviceId, ServiceMatchLevel.PRIMARY)
            }

        val portfolioPhotos =
            request.portfolioPhotoUrls.map { url ->
                PortfolioPhoto(UUID.randomUUID().toString(), url, null, Instant.now())
            }

        val completeness =
            if (
                request.description.isNotBlank() &&
                request.selectedServiceIds.isNotEmpty() &&
                !request.cityId.isNullOrBlank()
            ) {
                ProfileCompleteness.COMPLETE
            } else {
                ProfileCompleteness.INCOMPLETE
            }

        val profile =
            ProfessionalProfile(
                id = profileId,
                userId = userId,
                knownName = null,
                description = request.description,
                normalizedDescription = request.description,
                cityId = request.cityId,
                services = services,
                portfolioPhotos = portfolioPhotos,
                completeness = completeness,
                status = ProfessionalProfileStatus.PUBLISHED,
                lastActiveAt = Instant.now(),
                createdAt = existingProfile?.createdAt ?: Instant.now(),
                updatedAt = Instant.now(),
            )

        val savedProfile = profileRepository.save(profile)

        val phone = phoneAuthRepository.findByUserId(userId)?.phoneNumber ?: ""
        return profileResponseMapper.toResponse(savedProfile, user.fullName, user.photoUrl, phone, includeServiceStatus = true)
    }
}

class GetMyProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(userId: UserId): ProfessionalProfileResponse? {
        val profile = profileRepository.findByUserId(userId) ?: return null
        val user = userRepository.findById(userId)
        val phone = phoneAuthRepository.findByUserId(userId)?.phoneNumber ?: ""
        return profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone, includeServiceStatus = true)
    }
}

class GetPublicProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(profileId: ProfessionalProfileId): ProfessionalProfileResponse? {
        val profile = profileRepository.findById(profileId) ?: return null
        if (profile.status != ProfessionalProfileStatus.PUBLISHED) return null

        val user = userRepository.findById(profile.userId)
        val phone = phoneAuthRepository.findByUserId(profile.userId)?.phoneNumber ?: ""
        return profileResponseMapper.toResponse(profile, user?.fullName ?: "", user?.photoUrl, phone, includeServiceStatus = true)
    }
}

sealed class DisableProfileResult {
    object Success : DisableProfileResult()

    object NotFound : DisableProfileResult()

    object AlreadyInactive : DisableProfileResult()
}

class DisableProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun execute(userId: UserId): DisableProfileResult {
        val existing = profileRepository.findByUserId(userId) ?: return DisableProfileResult.NotFound
        if (existing.status == ProfessionalProfileStatus.INACTIVE) return DisableProfileResult.AlreadyInactive

        profileRepository.updateStatus(existing.id, ProfessionalProfileStatus.INACTIVE)
        return DisableProfileResult.Success
    }
}

sealed class UpdateProfileResult {
    data class Success(
        val response: ProfessionalProfileResponse,
    ) : UpdateProfileResult()

    object NotFound : UpdateProfileResult()

    object Blocked : UpdateProfileResult()
}

class UpdateProfessionalProfileService(
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val cityService: CityService,
    private val profileResponseMapper: ProfileResponseMapper,
) {
    fun execute(
        userId: UserId,
        request: ConfirmProfessionalProfileRequest,
    ): UpdateProfileResult {
        val existing = profileRepository.findByUserId(userId) ?: return UpdateProfileResult.NotFound
        if (existing.status == ProfessionalProfileStatus.BLOCKED) return UpdateProfileResult.Blocked

        if (!request.cityId.isNullOrBlank()) {
            require(cityService.findById(request.cityId!!) != null) { "INVALID_CITY_ID" }
        }

        val user = userRepository.findById(userId) ?: return UpdateProfileResult.NotFound

        val services =
            request.selectedServiceIds.map { serviceId ->
                ProfessionalProfileService(serviceId, ServiceMatchLevel.PRIMARY)
            }

        val portfolioPhotos =
            request.portfolioPhotoUrls.map { url ->
                PortfolioPhoto(UUID.randomUUID().toString(), url, null, Instant.now())
            }

        val completeness =
            if (
                request.description.isNotBlank() &&
                request.selectedServiceIds.isNotEmpty() &&
                !request.cityId.isNullOrBlank()
            ) {
                ProfileCompleteness.COMPLETE
            } else {
                ProfileCompleteness.INCOMPLETE
            }

        // Auto-disable when all services are removed; auto-reactivate when services are added back.
        val newStatus =
            when {
                services.isEmpty() -> ProfessionalProfileStatus.INACTIVE
                existing.status == ProfessionalProfileStatus.INACTIVE && services.isNotEmpty() -> ProfessionalProfileStatus.PUBLISHED
                else -> existing.status
            }

        val updated =
            existing.copy(
                description = request.description,
                normalizedDescription = request.description,
                cityId = request.cityId,
                services = services,
                portfolioPhotos = portfolioPhotos,
                completeness = completeness,
                status = newStatus,
                lastActiveAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val saved = profileRepository.save(updated)

        val phone = phoneAuthRepository.findByUserId(userId)?.phoneNumber ?: ""
        return UpdateProfileResult.Success(
            profileResponseMapper.toResponse(saved, user.fullName, user.photoUrl, phone, includeServiceStatus = true),
        )
    }
}
