package com.fugisawa.quemfaz.auth.token

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fugisawa.quemfaz.config.JwtConfig
import com.fugisawa.quemfaz.core.id.UserId
import java.util.*

class TokenService(private val config: JwtConfig) {
    fun generateToken(userId: UserId): String {
        return JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", userId.value)
            .withExpiresAt(Date(System.currentTimeMillis() + config.expiresInMs))
            .sign(Algorithm.HMAC256(config.secret))
    }
}
