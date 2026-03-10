package com.fugisawa.quemfaz.config

import com.fugisawa.quemfaz.environment.AppEnvironment
import com.fugisawa.quemfaz.auth.application.CompleteUserProfileService
import com.fugisawa.quemfaz.auth.application.StartOtpService
import com.fugisawa.quemfaz.auth.application.VerifyOtpService
import com.fugisawa.quemfaz.auth.domain.OtpChallengeRepository
import com.fugisawa.quemfaz.auth.domain.OtpHasher
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedOtpChallengeRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedUserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedUserRepository
import com.fugisawa.quemfaz.auth.infrastructure.Sha256OtpHasher
import com.fugisawa.quemfaz.auth.token.TokenService
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import com.fugisawa.quemfaz.infrastructure.otp.DefaultOtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.otp.OtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.otp.OtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.otp.RandomOtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.sms.AwsSmsSender
import com.fugisawa.quemfaz.infrastructure.sms.FakeSmsSender
import com.fugisawa.quemfaz.infrastructure.sms.SmsSender
import com.fugisawa.quemfaz.profile.application.*
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ExposedProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.interpretation.MockProfessionalInputInterpreter
import com.fugisawa.quemfaz.profile.interpretation.ProfessionalInputInterpreter
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

    // Auth Repositories
    single<UserRepository> { ExposedUserRepository() }
    single<UserPhoneAuthIdentityRepository> { ExposedUserPhoneAuthIdentityRepository() }
    single<OtpChallengeRepository> { ExposedOtpChallengeRepository() }

    // Auth Utilities
    single<OtpHasher> { Sha256OtpHasher() }
    single<TokenService> { TokenService(get<AppConfig>().jwt) }

    // Auth Services
    single { StartOtpService(get(), get(), get(), get(), get(), get<AppConfig>().otp) }
    single { VerifyOtpService(get(), get(), get(), get(), get()) }
    single { CompleteUserProfileService(get(), get()) }

    // Professional Profile Repositories
    single<ProfessionalProfileRepository> { ExposedProfessionalProfileRepository() }

    // Professional Profile Interpretation
    single<ProfessionalInputInterpreter> { MockProfessionalInputInterpreter() }

    // Professional Profile Services
    single { CreateProfessionalProfileDraftService(get()) }
    single { ConfirmProfessionalProfileService(get(), get()) }
    single { GetMyProfessionalProfileService(get(), get()) }
    single { GetPublicProfessionalProfileService(get(), get()) }
}
