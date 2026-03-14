package com.fugisawa.quemfaz.auth.application

import com.fugisawa.quemfaz.auth.domain.RefreshToken
import com.fugisawa.quemfaz.auth.domain.RefreshTokenRepository
import com.fugisawa.quemfaz.auth.token.TokenService
import com.fugisawa.quemfaz.contract.auth.RefreshTokenRequest
import com.fugisawa.quemfaz.contract.auth.RefreshTokenResponse
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenService: TokenService,
) {
    fun refresh(request: RefreshTokenRequest): RefreshTokenResponse =
        transaction {
            val oldRefreshToken =
                refreshTokenRepository.findByToken(request.refreshToken)
                    ?: return@transaction RefreshTokenResponse(
                        success = false,
                        token = "",
                        refreshToken = "",
                        message = "Invalid refresh token",
                    )

            if (oldRefreshToken.isRevoked()) {
                // Potential reuse detected! Revoke ALL tokens for this user as a precaution.
                refreshTokenRepository.revokeByUserId(oldRefreshToken.userId)
                return@transaction RefreshTokenResponse(
                    success = false,
                    token = "",
                    refreshToken = "",
                    message = "Refresh token reuse detected. All sessions revoked.",
                )
            }

            if (oldRefreshToken.isExpired()) {
                return@transaction RefreshTokenResponse(
                    success = false,
                    token = "",
                    refreshToken = "",
                    message = "Refresh token expired",
                )
            }

            // Rotate tokens
            refreshTokenRepository.revokeByToken(oldRefreshToken.token)

            val newAccessToken = tokenService.generateToken(oldRefreshToken.userId)
            val newRefreshTokenValue = tokenService.generateRefreshToken()
            val newRefreshToken =
                RefreshToken(
                    token = newRefreshTokenValue,
                    userId = oldRefreshToken.userId,
                    expiresAt = Instant.now().plusMillis(tokenService.getRefreshTokenExpiration()),
                    createdAt = Instant.now(),
                )

            refreshTokenRepository.create(newRefreshToken)

            RefreshTokenResponse(
                success = true,
                token = newAccessToken,
                refreshToken = newRefreshTokenValue,
            )
        }
    fun revoke(token: String) =
        transaction {
            refreshTokenRepository.revokeByToken(token)
        }
}
