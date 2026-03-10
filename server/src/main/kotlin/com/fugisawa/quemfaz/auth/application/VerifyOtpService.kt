package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.*
import com.fugisawa.quemfaz.auth.token.TokenService
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpResponse
import com.fugisawa.quemfaz.core.id.UserId
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class VerifyOtpService(
    private val userRepository: UserRepository,
    private val userPhoneAuthIdentityRepository: UserPhoneAuthIdentityRepository,
    private val otpChallengeRepository: OtpChallengeRepository,
    private val otpHasher: OtpHasher,
    private val tokenService: TokenService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun verifyOtp(request: VerifyOtpRequest): VerifyOtpResult = transaction {
        val normalizedPhone = PhoneNormalizer.normalize(request.phoneNumber)
        val challenge = otpChallengeRepository.findLatestActiveByPhoneNumber(normalizedPhone)
            ?: return@transaction VerifyOtpResult.Failure("No active challenge found")

        if (challenge.isMaxAttemptsReached()) {
            return@transaction VerifyOtpResult.Failure("Too many attempts")
        }

        if (!otpHasher.verify(request.otpCode, challenge.otpCodeHash)) {
            otpChallengeRepository.incrementAttemptCount(challenge.id)
            return@transaction VerifyOtpResult.Failure("Invalid code")
        }

        otpChallengeRepository.markConsumed(challenge.id, Instant.now())

        val identity = userPhoneAuthIdentityRepository.findByPhoneNumber(normalizedPhone)
        if (identity != null) {
            val user = userRepository.findById(identity.userId)
            if (user == null) {
                VerifyOtpResult.Failure("User not found for identity")
            } else if (user.status == UserStatus.BLOCKED) {
                VerifyOtpResult.Failure("User is blocked")
            } else {
                val token = tokenService.generateToken(user.id)
                VerifyOtpResult.Success(
                    response = VerifyOtpResponse(
                        success = true,
                        userId = user.id.value,
                        isNewUser = false,
                        requiresProfileCompletion = user.name == null
                    ),
                    token = token
                )
            }
        } else {
            // First time login
            val userId = UserId(UUID.randomUUID().toString())
            val newUser = User(
                id = userId,
                name = null,
                photoUrl = null,
                status = UserStatus.ACTIVE,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            userRepository.create(newUser)

            val newIdentity = UserPhoneAuthIdentity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                phoneNumber = normalizedPhone,
                isVerified = true,
                verifiedAt = Instant.now(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            userPhoneAuthIdentityRepository.create(newIdentity)

            val token = tokenService.generateToken(userId)
            VerifyOtpResult.Success(
                response = VerifyOtpResponse(
                    success = true,
                    userId = userId.value,
                    isNewUser = true,
                    requiresProfileCompletion = true
                ),
                token = token
            )
        }
    }
}

sealed class VerifyOtpResult {
    data class Success(val response: VerifyOtpResponse, val token: String) : VerifyOtpResult()
    data class Failure(val message: String) : VerifyOtpResult()
}
