package com.fugisawa.quemfaz.integration.catalog

import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogEndpointIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = emptyList()

    @Test
    fun `GET catalog returns active services and categories`() = integrationTestApplication {
        val client = createTestClient()
        val response = client.get("/services/catalog")
        assertEquals(HttpStatusCode.OK, response.status)

        val catalog = response.body<CatalogResponse>()
        assertEquals(7, catalog.categories.size)
        assertEquals(22, catalog.services.size)
        assertTrue(catalog.version.isNotBlank())
        assertTrue(catalog.services.none { it.id == "other-general" })
    }

    @Test
    fun `GET catalog returns 304 when ETag matches`() = integrationTestApplication {
        val client = createTestClient()
        val firstResponse = client.get("/services/catalog")
        val etag = firstResponse.headers[HttpHeaders.ETag]!!

        val secondResponse = client.get("/services/catalog") {
            header("If-None-Match", etag)
        }
        assertEquals(HttpStatusCode.NotModified, secondResponse.status)
    }
}
