package com.fugisawa.quemfaz.infrastructure.otp

interface OtpCodeGenerator {
    fun generate(): String
}

interface OtpMessageComposer {
    fun compose(code: String): String
}
