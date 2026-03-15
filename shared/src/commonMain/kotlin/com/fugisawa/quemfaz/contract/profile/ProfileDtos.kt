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
    val matchLevel: String,
    val status: String = "active",
)

@Serializable
data class CreateProfessionalProfileDraftRequest(
    val inputText: String,
    val inputMode: InputMode
)

@Serializable
data class CreateProfessionalProfileDraftResponse(
    val normalizedDescription: String,
    val editedDescription: String = "",
    val interpretedServices: List<InterpretedServiceDto>,
    val cityName: String?,
    val missingFields: List<String>,
    val followUpQuestions: List<String>,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
    val blockedDescriptions: List<String> = emptyList(),
)

@Serializable
data class ClarificationAnswer(
    val question: String,
    val answer: String,
)

@Serializable
data class ClarifyDraftRequest(
    val originalDescription: String,
    val clarificationAnswers: List<ClarificationAnswer>,
    val inputMode: InputMode = InputMode.TEXT,
)

@Serializable
data class ConfirmProfessionalProfileRequest(
    val normalizedDescription: String,
    val selectedServiceIds: List<String>,
    val cityName: String?,
    val contactPhone: String,
    val whatsAppPhone: String?,
    val portfolioPhotoUrls: List<String>
)

@Serializable
data class ProfessionalProfileResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val knownName: String?,
    val photoUrl: String?,
    val description: String,
    val cityName: String,
    val services: List<InterpretedServiceDto>,
    val profileComplete: Boolean,
    val activeRecently: Boolean,
    val whatsAppPhone: String?,
    val contactPhone: String,
    val portfolioPhotoUrls: List<String> = emptyList(),
    val contactCount: Int = 0,
    val daysSinceActive: Int? = null,
)

@Serializable
data class SetKnownNameRequest(
    val knownName: String?,
)
