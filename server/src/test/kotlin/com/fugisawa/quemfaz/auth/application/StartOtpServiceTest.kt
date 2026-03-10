package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.OtpChallenge
import com.fugisawa.quemfaz.auth.domain.OtpChallengeRepository
import com.fugisawa.quemfaz.auth.domain.OtpHasher
import com.fugisawa.quemfaz.config.OtpConfig
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.infrastructure.otp.OtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.otp.OtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.sms.SmsSender
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartOtpServiceTest {
    private class FakeOtpChallengeRepository : OtpChallengeRepository {
        var challenge: OtpChallenge? = null

        override fun create(challenge: OtpChallenge): OtpChallenge {
            this.challenge = challenge
            return challenge
        }

        override fun findLatestActiveByPhoneNumber(phoneNumber: String): OtpChallenge? = null

        override fun markConsumed(
            id: String,
            consumedAt: Instant,
        ): Boolean = false

        override fun incrementAttemptCount(id: String): Int = 0
    }

    private class FakeSmsSender : SmsSender {
        var lastPhone: String? = null
        var lastMessage: String? = null

        override suspend fun sendSms(
            phoneNumber: String,
            message: String,
        ) {
            lastPhone = phoneNumber
            lastMessage = message
        }
    }

    @Test
    fun `should start otp correctly`() =
        runBlocking {
            val otpRepo = FakeOtpChallengeRepository()
            val smsSender = FakeSmsSender()
            val service =
                StartOtpService(
                    otpRepo,
                    object : OtpCodeGenerator {
                        override fun generate() = "123456"
                    },
                    object : OtpHasher {
                        override fun hash(otpCode: String) = "hashed_$otpCode"

                        override fun verify(
                            otpCode: String,
                            hash: String,
                        ) = false
                    },
                    object : OtpMessageComposer {
                        override fun compose(code: String) = "Code is $code"
                    },
                    smsSender,
                    OtpConfig(6, 5),
                )

            val phone = "11999999999"
            val response = service.startOtp(StartOtpRequest(phone))

            assertTrue(response.success)
            assertEquals("5511999999999", otpRepo.challenge?.phoneNumber)
            assertEquals("hashed_123456", otpRepo.challenge?.otpCodeHash)
            assertEquals("5511999999999", smsSender.lastPhone)
            assertEquals("Code is 123456", smsSender.lastMessage)
        }
}
