package com.fugisawa.quemfaz.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("OnboardingInterpretation")
data class OnboardingInterpretation(
    val serviceIds: List<String>,
    val city: String? = null,
    val neighborhoods: List<String> = emptyList(),
    val needsClarification: Boolean,
    val clarificationQuestions: List<String> = emptyList(),
)

@Serializable
@SerialName("SearchInterpretation")
data class SearchInterpretation(
    val serviceId: String,
    val city: String? = null,
    val neighborhoods: List<String> = emptyList(),
)
