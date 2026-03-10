package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.core.id.UserId
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class CompleteUserProfileService(
    private val userRepository: UserRepository,
    private val userPhoneAuthIdentityRepository: UserPhoneAuthIdentityRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun completeProfile(
        userId: UserId,
        request: CompleteUserProfileRequest,
    ): CompleteProfileResult =
        transaction {
            val user = userRepository.findById(userId) ?: return@transaction CompleteProfileResult.Failure("User not found")

            userRepository.updateProfile(userId, request.name, request.photoUrl)

            val identity = userPhoneAuthIdentityRepository.findByUserId(userId)

            CompleteProfileResult.Success(
                UserProfileResponse(
                    id = user.id.value,
                    phoneNumber = identity?.phoneNumber ?: "unknown",
                    name = request.name,
                    photoUrl = request.photoUrl,
                    cityName = null,
                    status = user.status.name,
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
