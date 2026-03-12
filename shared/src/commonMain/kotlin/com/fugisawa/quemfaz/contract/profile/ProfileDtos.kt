package com.fugisawa.quemfaz.contract.profile

import kotlinx.serialization.Serializable

@Serializable
enum class InputMode {
    TEXT,
    VOICE
}

@Serializable
data class InterpretedServiceDto(
    val serviceId: String,
    val displayName: String,
    val matchLevel: String
)

@Serializable
data class CreateProfessionalProfileDraftRequest(
    val inputText: String,
    val inputMode: InputMode
)

@Serializable
data class CreateProfessionalProfileDraftResponse(
    val normalizedDescription: String,
    val interpretedServices: List<InterpretedServiceDto>,
    val cityName: String?,
    val neighborhoods: List<String>,
    val missingFields: List<String>,
    val followUpQuestions: List<String>,
    val freeTextAliases: List<String>
)

@Serializable
data class ConfirmProfessionalProfileRequest(
    val normalizedDescription: String,
    val selectedServiceIds: List<String>,
    val cityName: String?,
    val neighborhoods: List<String>,
    val contactPhone: String,
    val whatsAppPhone: String?,
    val photoUrl: String?,
    val portfolioPhotoUrls: List<String>
)

@Serializable
data class ProfessionalProfileResponse(
    val id: String,
    val name: String?,
    val photoUrl: String?,
    val description: String,
    val cityName: String,
    val neighborhoods: List<String>,
    val services: List<InterpretedServiceDto>,
    val profileComplete: Boolean,
    val activeRecently: Boolean,
    val whatsAppPhone: String?,
    val contactPhone: String,
    val portfolioPhotoUrls: List<String> = emptyList()
)
