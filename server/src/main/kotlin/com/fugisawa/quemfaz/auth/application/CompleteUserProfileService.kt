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
            if (request.firstName.isBlank() || request.lastName.isBlank()) {
                return@transaction CompleteProfileResult.Failure("First name and last name are required")
            }

            val user = userRepository.findById(userId) ?: return@transaction CompleteProfileResult.Failure("User not found")

            userRepository.updateName(userId, request.firstName.trim(), request.lastName.trim())

            val identity = userPhoneAuthIdentityRepository.findByUserId(userId)
            val profileExists = profileRepository.findByUserId(userId) != null

            CompleteProfileResult.Success(
                UserProfileResponse(
                    id = user.id.value,
                    phoneNumber = identity?.phoneNumber ?: "unknown",
                    firstName = request.firstName,
                    lastName = request.lastName,
                    photoUrl = null,
                    cityName = null,
                    status = user.status.name,
                    hasProfessionalProfile = profileExists,
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
