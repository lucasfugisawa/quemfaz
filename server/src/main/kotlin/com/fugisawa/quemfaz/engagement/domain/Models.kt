package com.fugisawa.quemfaz.engagement.domain

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import java.time.Instant

enum class ContactChannel {
    WHATSAPP,
    PHONE_CALL,
}

data class ContactClickEvent(
    val id: String,
    val professionalProfileId: ProfessionalProfileId,
    val userId: UserId?,
    val channel: ContactChannel,
    val cityName: String?,
    val source: String?,
    val createdAt: Instant,
)

interface ContactClickEventRepository {
    fun save(event: ContactClickEvent)
}
