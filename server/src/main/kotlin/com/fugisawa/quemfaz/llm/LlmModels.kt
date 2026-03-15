package com.fugisawa.quemfaz.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnmatchedDescription(
    val rawDescription: String,
    val safetyClassification: String = "safe",
    val safetyReason: String? = null,
)

@Serializable
@SerialName("OnboardingInterpretation")
data class OnboardingInterpretation(
    val serviceIds: List<String>,
    val needsClarification: Boolean,
    val clarificationQuestions: List<String> = emptyList(),
    val unmatchedDescriptions: List<UnmatchedDescription> = emptyList(),
)

@Serializable
@SerialName("SearchInterpretation")
data class SearchInterpretation(
    val serviceIds: List<String> = emptyList(),
    val unmatchedDescriptions: List<UnmatchedDescription> = emptyList(),
)
