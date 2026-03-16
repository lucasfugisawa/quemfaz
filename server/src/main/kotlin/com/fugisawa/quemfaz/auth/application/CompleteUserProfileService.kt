package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import org.jetbrains.exposed.sql.transactions.transaction

class CompleteUserProfileService(
    private val userRepository: UserRepository,
    private val userPhoneAuthIdentityRepository: UserPhoneAuthIdentityRepository,
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun completeProfile(
        userId: UserId,
        request: CompleteUserProfileRequest,
    ): CompleteProfileResult =
        transaction {
            val trimmedName = request.fullName.trim()
            if (trimmedName.split("\\s+".toRegex()).size < 2) {
                return@transaction CompleteProfileResult.Failure("Full name must contain at least first and last name")
            }

            val user = userRepository.findById(userId) ?: return@transaction CompleteProfileResult.Failure("User not found")

            userRepository.updateName(userId, trimmedName)

            val identity = userPhoneAuthIdentityRepository.findByUserId(userId)
            val profileExists = profileRepository.findByUserId(userId) != null

            CompleteProfileResult.Success(
                UserProfileResponse(
                    id = user.id.value,
                    phoneNumber = identity?.phoneNumber ?: "unknown",
                    fullName = trimmedName,
                    photoUrl = null,
                    cityName = null,
                    status = user.status.name,
                    hasProfessionalProfile = profileExists,
                    dateOfBirth = user.dateOfBirth?.toString(),
                ),
            )
        }
}

sealed class CompleteProfileResult {
    data class Success(
        val response: UserProfileResponse,
    ) : CompleteProfileResult()

    data class Failure(
        val message: String,
    ) : CompleteProfileResult()
}
