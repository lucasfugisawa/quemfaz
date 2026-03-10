package com.fugisawa.quemfaz.contract.engagement

import kotlinx.serialization.Serializable

@Serializable
enum class ContactChannelDto {
    WHATSAPP,
    PHONE_CALL
}

@Serializable
data class TrackContactClickRequest(
    val professionalProfileId: String,
    val channel: ContactChannelDto,
    val source: String? = null
)
