package com.fugisawa.quemfaz.config

import com.fugisawa.quemfaz.environment.AppEnvironment

data class AppConfig(
    val environment: AppEnvironment,
    val database: DatabaseConfig,
    val sms: SmsConfig,
    val otp: OtpConfig,
    val jwt: JwtConfig,
    val admin: AdminConfig,
)

data class AdminConfig(
    val adminUserIds: List<String>
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiresInMs: Long,
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val pass: String,
    val jdbcUrl: String = "jdbc:postgresql://$host:$port/$name"
)

data class SmsConfig(
    val provider: SmsProviderType,
    val aws: AwsSmsConfig? = null
)

enum class SmsProviderType {
    FAKE,
    AWS
}

data class AwsSmsConfig(
    val region: String,
    val senderId: String? = null
)

data class OtpConfig(
    val codeLength: Int = 6,
    val expirationMinutes: Int = 5
)
