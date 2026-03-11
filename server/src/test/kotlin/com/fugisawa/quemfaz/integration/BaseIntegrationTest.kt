package com.fugisawa.quemfaz.integration

import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.config.ConfigLoader
import com.fugisawa.quemfaz.config.DatabaseConfig
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import com.fugisawa.quemfaz.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
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
}
