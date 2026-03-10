package com.fugisawa.quemfaz.auth.domain

import com.fugisawa.quemfaz.core.id.UserId
import java.time.Instant

data class User(
    val id: UserId,
    val name: String?,
    val photoUrl: String?,
    val status: UserStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class UserStatus {
    ACTIVE,
    BLOCKED,
}

data class UserPhoneAuthIdentity(
    val id: String,
    val userId: UserId,
    val phoneNumber: String,
    val isVerified: Boolean,
    val verifiedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class OtpChallenge(
    val id: String,
    val phoneNumber: String,
    val otpCodeHash: String,
    val expiresAt: Instant,
    val attemptCount: Int,
    val maxAttempts: Int,
    val consumedAt: Instant?,
    val createdAt: Instant,
) {
    fun isExpired(now: Instant = Instant.now()) = expiresAt.isBefore(now)

    fun isConsumed() = consumedAt != null

    fun isMaxAttemptsReached() = attemptCount >= maxAttempts
}
