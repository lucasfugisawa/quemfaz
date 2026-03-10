package com.fugisawa.quemfaz.auth.infrastructure

import com.fugisawa.quemfaz.auth.domain.OtpHasher
import java.security.MessageDigest
import java.util.Base64

class Sha256OtpHasher : OtpHasher {
    override fun hash(otpCode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(otpCode.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    override fun verify(
        otpCode: String,
        hash: String,
    ): Boolean = hash(otpCode) == hash
}
