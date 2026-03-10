package com.fugisawa.quemfaz.auth.domain

interface OtpHasher {
    fun hash(otpCode: String): String

    fun verify(
        otpCode: String,
        hash: String,
    ): Boolean
}
