package com.fugisawa.quemfaz.contract.auth

import kotlinx.serialization.Serializable

@Serializable
data class StartOtpRequest(
    val phoneNumber: String
)

@Serializable
data class StartOtpResponse(
    val success: Boolean,
    val maskedDestination: String?,
    val message: String
)

@Serializable
data class VerifyOtpRequest(
    val phoneNumber: String,
    val otpCode: String
)

@Serializable
data class VerifyOtpResponse(
    val success: Boolean,
    val userId: String,
    val isNewUser: Boolean,
    val requiresProfileCompletion: Boolean,
    val token: String
)

@Serializable
data class CompleteUserProfileRequest(
    val name: String,
    val photoUrl: String?
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val phoneNumber: String,
    val name: String?,
    val photoUrl: String?,
    val cityName: String?,
    val status: String
)
