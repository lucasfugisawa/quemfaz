package com.fugisawa.quemfaz.integration

import com.fugisawa.quemfaz.Greeting
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SmokeIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = emptyList()

    @Test
    fun `smoke test - root endpoint should return greeting`() =
        integrationTestApplication {
            val client = createTestClient()
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Ktor: ${Greeting().greet()}", response.bodyAsText())
        }
}
