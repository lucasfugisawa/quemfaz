package com.fugisawa.quemfaz.domain.user

import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.core.value.PersonName
import com.fugisawa.quemfaz.core.value.PhoneNumber
import com.fugisawa.quemfaz.core.value.PhotoUrl
import com.fugisawa.quemfaz.domain.city.UserCityContext
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    ACTIVE,
    BLOCKED
}

@Serializable
data class User(
    val id: UserId,
    val phoneNumber: PhoneNumber,
    val name: PersonName?,
    val photoUrl: PhotoUrl?,
    val cityContext: UserCityContext?,
    val status: UserStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class BlockedUserNotice(
    val title: String,
    val message: String,
    val contactLabel: String
)
