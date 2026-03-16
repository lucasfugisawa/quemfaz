package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import org.jetbrains.exposed.sql.transactions.transaction

private val INTERNAL_IMAGE_PATH_REGEX = Regex("^/api/images/[A-Za-z0-9_-]+$")

class SetProfilePhotoService(
    private val userRepository: UserRepository,
    private val phoneAuthRepository: UserPhoneAuthIdentityRepository,
    private val profileRepository: ProfessionalProfileRepository,
) {
    fun execute(userId: UserId, request: SetProfilePhotoRequest): SetPhotoResult {
        if (!INTERNAL_IMAGE_PATH_REGEX.matches(request.photoUrl)) {
            return SetPhotoResult.InvalidUrl
        }

        val user = userRepository.findById(userId) ?: return SetPhotoResult.NotFound

        userRepository.updatePhotoUrl(userId, request.photoUrl)

        val identity = phoneAuthRepository.findByUserId(userId)
        val profileExists = profileRepository.findByUserId(userId) != null

        return SetPhotoResult.Success(
            UserProfileResponse(
                id = user.id.value,
                phoneNumber = identity?.phoneNumber ?: "",
                fullName = user.fullName,
                photoUrl = request.photoUrl,
                cityName = null,
                status = user.status.name,
                hasProfessionalProfile = profileExists,
                dateOfBirth = user.dateOfBirth?.toString(),
            ),
        )
    }
}

sealed class SetPhotoResult {
    data class Success(val response: UserProfileResponse) : SetPhotoResult()
    object NotFound : SetPhotoResult()
    object InvalidUrl : SetPhotoResult()
}
