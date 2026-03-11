package com.fugisawa.quemfaz.infrastructure.otp

import com.fugisawa.quemfaz.config.OtpConfig
import com.fugisawa.quemfaz.config.SmsProviderType
import kotlin.random.Random

class RandomOtpCodeGenerator(
    private val config: OtpConfig,
    private val smsProvider: SmsProviderType,
) : OtpCodeGenerator {
    override fun generate(): String {
        if (smsProvider == SmsProviderType.FAKE) {
            return "123456"
        }
        return (1..config.codeLength)
            .map { Random.nextInt(0, 10) }
            .joinToString("")
    }
}

class DefaultOtpMessageComposer : OtpMessageComposer {
    override fun compose(code: String): String = "Your QuemFaz verification code is: $code. Valid for 5 minutes."
}
