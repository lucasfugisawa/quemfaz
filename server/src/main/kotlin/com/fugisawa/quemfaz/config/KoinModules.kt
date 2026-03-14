package com.fugisawa.quemfaz.config

import com.fugisawa.quemfaz.auth.application.CompleteUserProfileService
import com.fugisawa.quemfaz.auth.application.GetAuthenticatedUserService
import com.fugisawa.quemfaz.auth.application.RefreshTokenService
import com.fugisawa.quemfaz.auth.application.SetProfilePhotoService
import com.fugisawa.quemfaz.auth.application.StartOtpService
import com.fugisawa.quemfaz.auth.application.VerifyOtpService
import com.fugisawa.quemfaz.auth.domain.OtpChallengeRepository
import com.fugisawa.quemfaz.auth.domain.OtpHasher
import com.fugisawa.quemfaz.auth.domain.RefreshTokenRepository
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedOtpChallengeRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedRefreshTokenRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedUserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.infrastructure.ExposedUserRepository
import com.fugisawa.quemfaz.auth.infrastructure.Sha256OtpHasher
import com.fugisawa.quemfaz.auth.token.TokenService
import com.fugisawa.quemfaz.engagement.application.TrackContactClickService
import com.fugisawa.quemfaz.engagement.application.TrackProfileViewService
import com.fugisawa.quemfaz.engagement.domain.ContactClickEventRepository
import com.fugisawa.quemfaz.engagement.domain.ProfileViewEventRepository
import com.fugisawa.quemfaz.engagement.infrastructure.ExposedContactClickEventRepository
import com.fugisawa.quemfaz.engagement.infrastructure.ExposedProfileViewEventRepository
import com.fugisawa.quemfaz.environment.AppEnvironment
import com.fugisawa.quemfaz.favorites.application.AddFavoriteService
import com.fugisawa.quemfaz.favorites.application.ListFavoritesService
import com.fugisawa.quemfaz.favorites.application.RemoveFavoriteService
import com.fugisawa.quemfaz.favorites.domain.FavoriteRepository
import com.fugisawa.quemfaz.favorites.infrastructure.ExposedFavoriteRepository
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import com.fugisawa.quemfaz.infrastructure.images.DatabaseImageStorageService
import com.fugisawa.quemfaz.infrastructure.images.ImageStorageService
import com.fugisawa.quemfaz.infrastructure.otp.DefaultOtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.otp.OtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.otp.OtpMessageComposer
import com.fugisawa.quemfaz.infrastructure.otp.RandomOtpCodeGenerator
import com.fugisawa.quemfaz.infrastructure.sms.AwsSmsSender
import com.fugisawa.quemfaz.infrastructure.sms.FakeSmsSender
import com.fugisawa.quemfaz.infrastructure.sms.SmsSender
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.moderation.application.CreateProfileReportService
import com.fugisawa.quemfaz.moderation.application.ModerationService
import com.fugisawa.quemfaz.moderation.domain.ReportRepository
import com.fugisawa.quemfaz.moderation.infrastructure.ExposedReportRepository
import com.fugisawa.quemfaz.profile.application.ClarifyProfessionalProfileDraftService
import com.fugisawa.quemfaz.profile.application.ConfirmProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.CreateProfessionalProfileDraftService
import com.fugisawa.quemfaz.profile.application.GetMyProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.GetPublicProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.SetKnownNameService
import com.fugisawa.quemfaz.profile.application.UpdateProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ExposedProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.interpretation.LlmProfessionalInputInterpreter
import com.fugisawa.quemfaz.profile.interpretation.ProfessionalInputInterpreter
import com.fugisawa.quemfaz.search.application.SearchProfessionalsService
import com.fugisawa.quemfaz.search.domain.SearchQueryRepository
import com.fugisawa.quemfaz.search.infrastructure.persistence.ExposedSearchQueryRepository
import com.fugisawa.quemfaz.search.interpretation.LlmSearchQueryInterpreter
import com.fugisawa.quemfaz.search.interpretation.SearchQueryInterpreter
import com.fugisawa.quemfaz.search.ranking.ProfessionalSearchRankingService
import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val configModule =
    module {
        single<AppConfig> {
            val ktorConfig = get<ApplicationConfig>()
            ConfigLoader.loadConfig(ktorConfig)
        }
    }

val infrastructureModule =
    module {
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

        // Image Storage (temporary DB-backed implementation — replace binding to migrate to S3)
        single<ImageStorageService> { DatabaseImageStorageService() }

        single<SmsSender> {
            val config = get<AppConfig>()
            val logger = LoggerFactory.getLogger("SmsProviderSelector")
            logger.info("Initializing SMS provider: ${config.sms.provider}")
            when (config.sms.provider) {
                SmsProviderType.FAKE -> {
                    FakeSmsSender()
                }

                SmsProviderType.AWS -> {
                    val awsConfig = config.sms.aws ?: throw IllegalStateException("AWS SMS config missing")
                    AwsSmsSender(awsConfig)
                }
            }
        }

        single<OtpCodeGenerator> {
            val config = get<AppConfig>()
            RandomOtpCodeGenerator(config.otp, config.sms.provider)
        }

        single<OtpMessageComposer> {
            DefaultOtpMessageComposer()
        }

        // Auth Repositories
        single<UserRepository> { ExposedUserRepository() }
        single<UserPhoneAuthIdentityRepository> { ExposedUserPhoneAuthIdentityRepository() }
        single<OtpChallengeRepository> { ExposedOtpChallengeRepository() }
        single<RefreshTokenRepository> { ExposedRefreshTokenRepository() }

        // Auth Utilities
        single<OtpHasher> { Sha256OtpHasher() }
        single<TokenService> { TokenService(get<AppConfig>().jwt) }

        // Auth Services
        single { StartOtpService(get(), get(), get(), get(), get(), get<AppConfig>().otp) }
        single { VerifyOtpService(get(), get(), get(), get(), get(), get()) }
        single { RefreshTokenService(get(), get()) }
        single { CompleteUserProfileService(get(), get(), get()) }
        single { GetAuthenticatedUserService(get(), get(), get()) }
        single { SetProfilePhotoService(get(), get(), get()) }

        // Professional Profile Repositories
        single<ProfessionalProfileRepository> { ExposedProfessionalProfileRepository() }

        // LLM Agent Service
        single { LlmAgentService() }

        // Professional Profile Interpretation
        single<ProfessionalInputInterpreter> { LlmProfessionalInputInterpreter(get()) }

        // Professional Profile Services
        single { CreateProfessionalProfileDraftService(get()) }
        single { ClarifyProfessionalProfileDraftService(get()) }
        single { ConfirmProfessionalProfileService(get(), get()) }
        single { GetMyProfessionalProfileService(get(), get()) }
        single { GetPublicProfessionalProfileService(get(), get()) }
        single { UpdateProfessionalProfileService(get(), get()) }
        single { SetKnownNameService(get()) }

        // Search Repositories
        single<SearchQueryRepository> { ExposedSearchQueryRepository() }

        // Search Interpretation
        single<SearchQueryInterpreter> { LlmSearchQueryInterpreter(get()) }

        // Search Ranking
        single { ProfessionalSearchRankingService() }

        // Search Services
        single { SearchProfessionalsService(get(), get(), get(), get(), get()) }

        // Favorites
        single<FavoriteRepository> { ExposedFavoriteRepository() }
        single { AddFavoriteService(get(), get()) }
        single { RemoveFavoriteService(get()) }
        single { ListFavoritesService(get(), get(), get()) }

        // Engagement
        single<ContactClickEventRepository> { ExposedContactClickEventRepository() }
        single { TrackContactClickService(get(), get()) }
        single<ProfileViewEventRepository> { ExposedProfileViewEventRepository() }
        single { TrackProfileViewService(get(), get()) }

        // Moderation
        single<ReportRepository> { ExposedReportRepository() }
        single { CreateProfileReportService(get(), get()) }
        single { ModerationService(get(), get(), get()) }
    }
