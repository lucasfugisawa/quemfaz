package com.fugisawa.quemfaz.config

import com.fugisawa.quemfaz.environment.AppEnvironment
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import com.fugisawa.quemfaz.infrastructure.otp.DefaultOtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.otp.OtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.otp.OtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.otp.RandomOtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.sms.AwsSmsSender
import com.fugisawa.quemfaz.infrastructure.sms.FakeSmsSender
import com.fugisawa.quemfaz.infrastructure.sms.SmsSender
import io.ktor.server.config.*
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val configModule = module {
    single<AppConfig> {
        val ktorConfig = get<ApplicationConfig>()
        ConfigLoader.loadConfig(ktorConfig)
    }
}

val infrastructureModule = module {
    single<DatabaseFactory> {
        val config = get<AppConfig>()
        DatabaseFactory(config.database)
    }

    single<DataSource> {
        val factory = get<DatabaseFactory>()
        val ds = factory.createDataSource()
        factory.runMigrations(ds)
        ds
    }

    single<SmsSender> {
        val config = get<AppConfig>()
        val logger = LoggerFactory.getLogger("SmsProviderSelector")
        logger.info("Initializing SMS provider: ${config.sms.provider}")
        when (config.sms.provider) {
            SmsProviderType.FAKE -> FakeSmsSender()
            SmsProviderType.AWS -> {
                val awsConfig = config.sms.aws ?: throw IllegalStateException("AWS SMS config missing")
                AwsSmsSender(awsConfig)
            }
        }
    }

    single<OtpCodeGenerator> {
        val config = get<AppConfig>()
        RandomOtpCodeGenerator(config.otp)
    }

    single<OtpMessageComposer> {
        DefaultOtpMessageComposer()
    }
}
