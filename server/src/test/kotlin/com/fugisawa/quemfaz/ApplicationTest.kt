package com.fugisawa.quemfaz

import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.config.ConfigLoader
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.mockito.kotlin.mock
import javax.sql.DataSource
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
            val testConfig =
                MapApplicationConfig(
                    "jwt.secret" to "test-secret",
                    "jwt.issuer" to "test-issuer",
                    "jwt.audience" to "test-audience",
                    "jwt.expiresInMs" to "3600000",
                    "db.host" to "localhost",
                    "db.port" to "5432",
                    "db.name" to "test",
                    "db.user" to "test",
                    "db.pass" to "test",
                    "sms.provider" to "FAKE",
                    "admin.userIds.size" to "0",
                )
            environment {
                config = testConfig
            }
            install(Koin) {
                modules(
                    module { single { testConfig } },
                    module {
                        single<AppConfig> { ConfigLoader.loadConfig(testConfig) }
                    },
                    module {
                        single<DataSource> { mock() }
                        single<DatabaseFactory> { mock() }
                    },
                )
            }
            application {
                module()
            }
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Ktor: ${Greeting().greet()}", response.bodyAsText())
        }
}
