package com.fugisawa.quemfaz.domain.user

import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.core.value.PersonName
import com.fugisawa.quemfaz.core.value.PhoneNumber
import com.fugisawa.quemfaz.core.value.PhotoUrl
import com.fugisawa.quemfaz.domain.city.UserCityContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Status of a user account.
 *
 * @property ACTIVE User can access the platform normally
 * @property BLOCKED User has been blocked and cannot access the platform
 */
@Serializable
enum class UserStatus {
    ACTIVE,
    BLOCKED
}

/**
 * Represents a user account on the platform.
 *
 * Users can be both service seekers and professionals (with profiles).
 * Authentication is phone-based (OTP).
 *
 * @property id Unique user identifier
 * @property phoneNumber Phone number for authentication and contact
 * @property name Optional display name
 * @property photoUrl Optional profile photo
 * @property cityContext User's current city selection for personalized results
 * @property status Account status
 * @property createdAt When the account was created
 * @property updatedAt When the account was last modified
 */
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

/**
 * Information to display to a blocked user.
 *
 * @property title Headline for the block notice
 * @property message Detailed explanation of why they're blocked
 * @property contactLabel Label for the support contact method
 */
@Serializable
data class BlockedUserNotice(
    val title: String,
    val message: String,
    val contactLabel: String
)
