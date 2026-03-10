package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.OtpChallenge
import com.fugisawa.quemfaz.auth.domain.OtpChallengeRepository
import com.fugisawa.quemfaz.auth.domain.OtpHasher
import com.fugisawa.quemfaz.auth.domain.PhoneNormalizer
import com.fugisawa.quemfaz.config.OtpConfig
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpResponse
import com.fugisawa.quemfaz.infrastructure.otp.OtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.otp.OtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.sms.SmsSender
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class StartOtpService(
    private val otpChallengeRepository: OtpChallengeRepository,
    private val otpCodeGenerator: OtpCodeGenerator,
    private val otpHasher: OtpHasher,
    private val otpMessageComposer: OtpMessageComposer,
    private val smsSender: SmsSender,
    private val otpConfig: OtpConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun startOtp(request: StartOtpRequest): StartOtpResponse =
        newSuspendedTransaction {
            val normalizedPhone = PhoneNormalizer.normalize(request.phoneNumber)
            val otpCode = otpCodeGenerator.generate()
            val otpHash = otpHasher.hash(otpCode)

            val challenge =
                OtpChallenge(
                    id = UUID.randomUUID().toString(),
                    phoneNumber = normalizedPhone,
                    otpCodeHash = otpHash,
                    expiresAt = Instant.now().plusSeconds(otpConfig.expirationMinutes.toLong() * 60),
                    attemptCount = 0,
                    maxAttempts = 3,
                    consumedAt = null,
                    createdAt = Instant.now(),
                )

            otpChallengeRepository.create(challenge)

            val message = otpMessageComposer.compose(otpCode)
            smsSender.sendSms(normalizedPhone, message)

            logger.info("OTP started for phone: ${maskPhone(normalizedPhone)}")

            StartOtpResponse(
                success = true,
                maskedDestination = maskPhone(normalizedPhone),
                message = "OTP sent successfully",
            )
        }

    private fun maskPhone(phone: String): String {
        if (phone.length < 4) return "****"
        return phone.take(phone.length - 4).map { '*' }.joinToString("") + phone.takeLast(4)
    }
}
