package com.fugisawa.quemfaz.config

import com.fugisawa.quemfaz.environment.AppEnvironment
import io.ktor.server.config.ApplicationConfig
import org.slf4j.LoggerFactory

object ConfigLoader {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun loadConfig(ktorConfig: ApplicationConfig): AppConfig {
        val envString = ktorConfig.propertyOrNull("app.env")?.getString() ?: "local"
        val environment = AppEnvironment.fromString(envString)

        logger.info("Loading configuration for environment: ${environment.value}")

        return AppConfig(
            environment = environment,
            database =
                DatabaseConfig(
                    host = ktorConfig.property("db.host").getString(),
                    port = ktorConfig.property("db.port").getString().toInt(),
                    name = ktorConfig.property("db.name").getString(),
                    user = ktorConfig.property("db.user").getString(),
                    pass = ktorConfig.property("db.pass").getString(),
                ),
            sms =
                SmsConfig(
                    provider =
                        SmsProviderType.valueOf(
                            ktorConfig.propertyOrNull("sms.provider")?.getString() ?: "FAKE",
                        ),
                    aws =
                        try {
                            val region = ktorConfig.propertyOrNull("sms.aws.region")?.getString()
                            if (region != null) {
                                AwsSmsConfig(
                                    region = region,
                                    senderId = ktorConfig.propertyOrNull("sms.aws.senderId")?.getString(),
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        },
                ),
            otp =
                OtpConfig(
                    codeLength = ktorConfig.propertyOrNull("otp.codeLength")?.getString()?.toInt() ?: 6,
                    expirationMinutes = ktorConfig.propertyOrNull("otp.expirationMinutes")?.getString()?.toInt() ?: 5,
                ),
            jwt =
                JwtConfig(
                    secret = ktorConfig.propertyOrNull("jwt.secret")?.getString() ?: "dev-secret",
                    issuer = ktorConfig.propertyOrNull("jwt.issuer")?.getString() ?: "quemfaz",
                    audience = ktorConfig.propertyOrNull("jwt.audience")?.getString() ?: "quemfaz-audience",
                    expiresInMs = ktorConfig.propertyOrNull("jwt.expiresInMs")?.getString()?.toLong() ?: 3600000L,
                ),
            admin =
                AdminConfig(
                    adminUserIds = ktorConfig.propertyOrNull("admin.userIds")?.getList() ?: emptyList(),
                ),
            llm =
                LlmConfig(
                    timeoutMs = ktorConfig.propertyOrNull("llm.timeoutMs")?.getString()?.toLong() ?: 8000L,
                ),
        )
    }
}
