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
    val token: String,
    val refreshToken: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val success: Boolean,
    val token: String,
    val refreshToken: String,
    val message: String? = null
)

@Serializable
data class CompleteUserProfileRequest(
    val fullName: String
)

@Serializable
data class SetProfilePhotoRequest(
    val photoUrl: String
)

@Serializable
data class UserProfileResponse(
    val id: String,
    val phoneNumber: String,
    val fullName: String,
    val photoUrl: String?,
    val cityId: String?,
    val cityName: String?,
    val status: String,
    val hasProfessionalProfile: Boolean = false,
    val dateOfBirth: String? = null,
    val termsAcceptedAt: String? = null,
    val termsVersion: String? = null,
    val privacyAcceptedAt: String? = null,
    val privacyVersion: String? = null,
    val requiredTermsVersion: String? = null,
    val requiredPrivacyVersion: String? = null,
)

@Serializable
data class UpdateDateOfBirthRequest(
    val dateOfBirth: String  // ISO-8601 format, e.g. "1990-05-15"
)

@Serializable
data class AcceptTermsRequest(
    val termsVersion: String,
    val privacyVersion: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)
