package com.fugisawa.quemfaz.integration

import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.config.ConfigLoader
import com.fugisawa.quemfaz.config.DatabaseConfig
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import com.fugisawa.quemfaz.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource

@Testcontainers
abstract class BaseIntegrationTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        @Container
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withDatabaseName("quemfaz_test")
                withUsername("test")
                withPassword("test")
                // start() is not needed with @Container + @Testcontainers
            }

        val databaseConfig: DatabaseConfig
            get() =
                DatabaseConfig(
                    host = postgres.host,
                    port = postgres.getMappedPort(5432),
                    name = postgres.databaseName,
                    user = postgres.username,
                    pass = postgres.password,
                )
    }

    protected abstract val tablesToClean: List<Table>

    @BeforeEach
    fun clearDatabase() {
    }

    protected fun integrationTestApplication(
        koinModules: List<org.koin.core.module.Module>? = null,
        block: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        val testConfig =
            MapApplicationConfig(
                "jwt.secret" to (System.getenv("TEST_JWT_SECRET") ?: "test-secret"),
                "jwt.issuer" to (System.getenv("TEST_JWT_ISSUER") ?: "test-issuer"),
                "jwt.audience" to (System.getenv("TEST_JWT_AUDIENCE") ?: "test-audience"),
                "jwt.expiresInMs" to (System.getenv("TEST_JWT_EXPIRES_IN") ?: "3600000"),
                "db.host" to databaseConfig.host,
                "db.port" to databaseConfig.port.toString(),
                "db.name" to databaseConfig.name,
                "db.user" to databaseConfig.user,
                "db.pass" to databaseConfig.pass,
                "sms.provider" to (System.getenv("TEST_SMS_PROVIDER") ?: "FAKE"),
                "admin.userIds.size" to "0",
            )
        environment {
            config = testConfig
        }
        install(Koin) {
            modules(
                koinModules ?: listOf(
                    module { single { testConfig } },
                    module {
                        single<AppConfig> { ConfigLoader.loadConfig(testConfig) }
                    },
                    com.fugisawa.quemfaz.config.infrastructureModule,
                ),
            )
        }
        application {
            module()

            // Run cleanup inside the application context to ensure DataSource is available
            if (tablesToClean.isNotEmpty()) {
                val dbFactory by inject<DatabaseFactory>()
                val dataSource by inject<DataSource>()
                dbFactory.connect(dataSource)

                transaction {
                    logger.info("Cleaning tables: ${tablesToClean.joinToString { it.tableName }}")
                    exec("SET CONSTRAINTS ALL DEFERRED")
                    tablesToClean.forEach { table ->
                        exec("TRUNCATE TABLE ${table.tableName} RESTART IDENTITY CASCADE")
                    }
                }
            }
        }

        block()
    }

    protected fun ApplicationTestBuilder.createTestClient(token: String? = null): HttpClient =
        createClient {
            install(ContentNegotiation) {
                json()
            }
            if (token != null) {
                install(DefaultRequest) {
                    header("Authorization", "Bearer $token")
                }
            }
        }

    protected suspend fun ApplicationTestBuilder.obtainAuthToken(phone: String): String {
        val client = createTestClient()
        client.post("/auth/start-otp") {
            contentType(ContentType.Application.Json)
            setBody(StartOtpRequest(phoneNumber = phone))
        }
        val verifyResponse = client.post("/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(phoneNumber = phone, otpCode = "123456"))
        }
        return verifyResponse.headers[HttpHeaders.Authorization]!!.removePrefix("Bearer ")
    }

    protected suspend fun ApplicationTestBuilder.completeNameStep(
        token: String,
        firstName: String,
        lastName: String,
    ) {
        createTestClient(token).post("/auth/profile") {
            contentType(ContentType.Application.Json)
            setBody(CompleteUserProfileRequest(firstName = firstName, lastName = lastName))
        }
    }

    protected suspend fun ApplicationTestBuilder.setUserPhoto(
        token: String,
        photoUrl: String,
    ) {
        createTestClient(token).post("/auth/photo") {
            contentType(ContentType.Application.Json)
            setBody(SetProfilePhotoRequest(photoUrl = photoUrl))
        }
    }

    protected suspend fun ApplicationTestBuilder.createAndConfirmProfile(token: String) {
        val client = createTestClient(token)
        val draftResponse = client.post("/professional-profile/draft") {
            contentType(ContentType.Application.Json)
            setBody(CreateProfessionalProfileDraftRequest(
                inputText = "Pintor residencial em São Paulo",
                inputMode = InputMode.TEXT,
            ))
        }
        val draft = draftResponse.body<CreateProfessionalProfileDraftResponse>()
        client.post("/professional-profile/confirm") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmProfessionalProfileRequest(
                normalizedDescription = draft.normalizedDescription,
                selectedServiceIds = draft.interpretedServices.map { it.serviceId },
                cityName = "São Paulo",
                contactPhone = "+5511999999999",
                whatsAppPhone = "+5511999999999",
                portfolioPhotoUrls = emptyList(),
            ))
        }
    }
}
