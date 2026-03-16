package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.OtpChallengeRepository
import com.fugisawa.quemfaz.auth.domain.OtpHasher
import com.fugisawa.quemfaz.auth.domain.PhoneNormalizer
import com.fugisawa.quemfaz.auth.domain.RefreshToken
import com.fugisawa.quemfaz.auth.domain.RefreshTokenRepository
import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentity
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.auth.token.TokenService
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpResponse
import com.fugisawa.quemfaz.core.id.UserId
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class VerifyOtpService(
    private val userRepository: UserRepository,
    private val userPhoneAuthIdentityRepository: UserPhoneAuthIdentityRepository,
    private val otpChallengeRepository: OtpChallengeRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val otpHasher: OtpHasher,
    private val tokenService: TokenService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun verifyOtp(request: VerifyOtpRequest): VerifyOtpResult =
        transaction {
            val normalizedPhone = PhoneNormalizer.normalize(request.phoneNumber)
            val challenge =
                otpChallengeRepository.findLatestActiveByPhoneNumber(normalizedPhone)
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
                    return@transaction VerifyOtpResult.Failure("User not found for identity")
                } else if (user.status == UserStatus.BLOCKED) {
                    return@transaction VerifyOtpResult.Blocked
                } else {
                    val token = tokenService.generateToken(user.id)
                    val refreshTokenValue = tokenService.generateRefreshToken()
                    val refreshToken =
                        RefreshToken(
                            token = refreshTokenValue,
                            userId = user.id,
                            expiresAt = Instant.now().plusMillis(tokenService.getRefreshTokenExpiration()),
                            createdAt = Instant.now(),
                        )
                    refreshTokenRepository.create(refreshToken)

                    return@transaction VerifyOtpResult.Success(
                        response =
                            VerifyOtpResponse(
                                success = true,
                                userId = user.id.value,
                                isNewUser = false,
                                requiresProfileCompletion = user.fullName.isBlank(),
                                token = token,
                                refreshToken = refreshTokenValue,
                            ),
                        token = token,
                    )
                }
            } else {
                // First time login
                val userId = UserId(UUID.randomUUID().toString())
                val newUser =
                    User(
                        id = userId,
                        fullName = "",
                        photoUrl = null,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )
                userRepository.create(newUser)

                val newIdentity =
                    UserPhoneAuthIdentity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        phoneNumber = normalizedPhone,
                        isVerified = true,
                        verifiedAt = Instant.now(),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    )
                userPhoneAuthIdentityRepository.create(newIdentity)

                val token = tokenService.generateToken(userId)
                val refreshTokenValue = tokenService.generateRefreshToken()
                val refreshToken =
                    RefreshToken(
                        token = refreshTokenValue,
                        userId = userId,
                        expiresAt = Instant.now().plusMillis(tokenService.getRefreshTokenExpiration()),
                        createdAt = Instant.now(),
                    )
                refreshTokenRepository.create(refreshToken)

                return@transaction VerifyOtpResult.Success(
                    response =
                        VerifyOtpResponse(
                            success = true,
                            userId = userId.value,
                            isNewUser = true,
                            requiresProfileCompletion = true,
                            token = token,
                            refreshToken = refreshTokenValue,
                        ),
                    token = token,
                )
            }
        }
}

sealed class VerifyOtpResult {
    data class Success(
        val response: VerifyOtpResponse,
        val token: String,
    ) : VerifyOtpResult()

    data class Failure(
        val message: String,
    ) : VerifyOtpResult()

    object Blocked : VerifyOtpResult()
}
