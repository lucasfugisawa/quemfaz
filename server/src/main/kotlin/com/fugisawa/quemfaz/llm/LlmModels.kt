package com.fugisawa.quemfaz.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("OnboardingInterpretation")
data class OnboardingInterpretation(
    val services: List<String>,
    val city: String? = null,
    val neighborhoods: List<String> = emptyList(),
    val needsClarification: Boolean,
    val clarificationQuestions: List<String> = emptyList(),
)

@Serializable
@SerialName("SearchInterpretation")
data class SearchInterpretation(
    val service: String,
    val city: String? = null,
    val neighborhoods: List<String> = emptyList(),
)
