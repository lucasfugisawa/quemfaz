package com.fugisawa.quemfaz.contract.common

import kotlinx.serialization.Serializable

@Serializable
data class ApiMessage(
    val code: String,
    val message: String
)

@Serializable
data class SimpleSuccessResponse(
    val success: Boolean,
    val message: ApiMessage? = null
)
