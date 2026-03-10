package com.fugisawa.quemfaz.infrastructure.otp

import com.fugisawa.quemfaz.config.OtpConfig
import kotlin.random.Random

class RandomOtpCodeGenerator(
    private val config: OtpConfig,
) : OtpCodeGenerator {
    override fun generate(): String =
        (1..config.codeLength)
            .map { Random.nextInt(0, 10) }
            .joinToString("")
}

class DefaultOtpMessageComposer : OtpMessageComposer {
    override fun compose(code: String): String = "Your QuemFaz verification code is: $code. Valid for 5 minutes."
}
