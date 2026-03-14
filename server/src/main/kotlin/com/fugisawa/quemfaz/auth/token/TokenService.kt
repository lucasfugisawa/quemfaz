package com.fugisawa.quemfaz.auth.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fugisawa.quemfaz.config.JwtConfig
import com.fugisawa.quemfaz.core.id.UserId
import java.security.SecureRandom
import java.util.Base64
import java.util.Date

class TokenService(
    private val config: JwtConfig,
) {
    private val secureRandom = SecureRandom()

    fun generateToken(userId: UserId): String =
        JWT
            .create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId.value)
            .withExpiresAt(Date(System.currentTimeMillis() + config.expiresInMs))
            .sign(Algorithm.HMAC256(config.secret))

    fun generateRefreshToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun getRefreshTokenExpiration(): Long = config.refreshExpiresInMs
}
