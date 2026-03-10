package com.fugisawa.quemfaz.auth.domain

import com.fugisawa.quemfaz.core.id.UserId
import java.time.Instant

interface UserRepository {
    fun create(user: User): User
    fun findById(id: UserId): User?
    fun updateProfile(id: UserId, name: String, photoUrl: String?): User?
}

interface UserPhoneAuthIdentityRepository {
    fun findByPhoneNumber(phoneNumber: String): UserPhoneAuthIdentity?
    fun findByUserId(userId: UserId): UserPhoneAuthIdentity?
    fun create(identity: UserPhoneAuthIdentity): UserPhoneAuthIdentity
    fun markVerified(id: String, verifiedAt: Instant): Boolean
}

interface OtpChallengeRepository {
    fun create(challenge: OtpChallenge): OtpChallenge
    fun findLatestActiveByPhoneNumber(phoneNumber: String): OtpChallenge?
    fun markConsumed(id: String, consumedAt: Instant): Boolean
    fun incrementAttemptCount(id: String): Int
}
