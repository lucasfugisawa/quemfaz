package com.fugisawa.quemfaz.integration.city

import com.fugisawa.quemfaz.contract.city.CitiesResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CityIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = emptyList()

    @Test
    fun `GET cities returns seeded active cities`() =
        integrationTestApplication {
            val response = createTestClient().get("/cities")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.body<CitiesResponse>()
            assertEquals(3, body.cities.size)

            val cityIds = body.cities.map { it.id }.toSet()
            assertTrue("batatais" in cityIds)
            assertTrue("franca" in cityIds)
            assertTrue("ribeirao-preto" in cityIds)

            val ribeirao = body.cities.first { it.id == "ribeirao-preto" }
            assertEquals("Ribeirão Preto", ribeirao.name)
            assertEquals("SP", ribeirao.stateCode)
        }
}
