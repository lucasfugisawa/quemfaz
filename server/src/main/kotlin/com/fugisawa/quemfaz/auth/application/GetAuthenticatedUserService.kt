package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.config.LegalConfig
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository

class GetAuthenticatedUserService(
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val legalConfig: LegalConfig,
) {
    fun execute(userId: UserId): UserProfileResponse {
        val user =
            userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found")

        val phoneIdentity = phoneAuthRepository.findByUserId(userId)
        val profile = profileRepository.findByUserId(userId)

        return UserProfileResponse(
            id = user.id.value,
            phoneNumber = phoneIdentity?.phoneNumber ?: "",
            fullName = user.fullName,
            photoUrl = user.photoUrl,
            cityName = null,
            status = user.status.name,
            hasProfessionalProfile = profile != null,
            dateOfBirth = user.dateOfBirth?.toString(),
            termsAcceptedAt = user.termsAcceptedAt?.toString(),
            termsVersion = user.termsVersion,
            privacyAcceptedAt = user.privacyAcceptedAt?.toString(),
            privacyVersion = user.privacyVersion,
            requiredTermsVersion = legalConfig.requiredTermsVersion,
            requiredPrivacyVersion = legalConfig.requiredPrivacyVersion,
        )
    }
}
