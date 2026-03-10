package com.fugisawa.quemfaz.core.error

import kotlinx.serialization.Serializable

@Serializable
data class ValidationIssue(
    val field: String,
    val message: String
)
