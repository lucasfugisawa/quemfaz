package com.fugisawa.quemfaz.integration.catalog

import com.fugisawa.quemfaz.catalog.infrastructure.persistence.CanonicalServicesTable
import com.fugisawa.quemfaz.contract.catalog.PendingServiceResponse
import com.fugisawa.quemfaz.contract.catalog.ReviewServiceRequest
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminCatalogIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = emptyList()

    private fun insertPendingService(id: String, displayName: String, categoryId: String = "CLEANING") {
        transaction {
            CanonicalServicesTable.insert {
                it[CanonicalServicesTable.id] = id
                it[CanonicalServicesTable.displayName] = displayName
                it[CanonicalServicesTable.description] = "Test service"
                it[CanonicalServicesTable.categoryId] = categoryId
                it[CanonicalServicesTable.aliases] = emptyList()
                it[CanonicalServicesTable.status] = "pending_review"
                it[CanonicalServicesTable.createdBy] = "test"
            }
        }
    }

    private fun cleanupService(id: String) {
        transaction {
            CanonicalServicesTable.deleteWhere { CanonicalServicesTable.id eq id }
        }
    }

    @Test
    fun `GET pending returns pending services`() = integrationTestApplication {
        val token = obtainAuthToken("+5511999000001")

        // Warm up the app to ensure DB is connected
        createTestClient().get("/services/catalog")

        insertPendingService("test-pending-1", "Serviço de Teste 1")

        try {
            val client = createTestClient(token)
            val response = client.get("/admin/catalog/pending")
            assertEquals(HttpStatusCode.OK, response.status)

            val pending = response.body<List<PendingServiceResponse>>()
            assertTrue(pending.any { it.id == "test-pending-1" })
        } finally {
            cleanupService("test-pending-1")
        }
    }

    @Test
    fun `POST approve changes service to active`() = integrationTestApplication {
        val token = obtainAuthToken("+5511999000002")

        createTestClient().get("/services/catalog")

        insertPendingService("test-approve-1", "Serviço Aprovável")

        try {
            val client = createTestClient(token)
            val response = client.post("/admin/catalog/test-approve-1/approve")
            assertEquals(HttpStatusCode.OK, response.status)

            // Verify it no longer appears in pending
            val pendingResponse = client.get("/admin/catalog/pending")
            val pending = pendingResponse.body<List<PendingServiceResponse>>()
            assertTrue(pending.none { it.id == "test-approve-1" })
        } finally {
            cleanupService("test-approve-1")
        }
    }

    @Test
    fun `POST reject changes service to rejected`() = integrationTestApplication {
        val token = obtainAuthToken("+5511999000003")

        createTestClient().get("/services/catalog")

        insertPendingService("test-reject-1", "Serviço Rejeitável")

        try {
            val client = createTestClient(token)
            val response = client.post("/admin/catalog/test-reject-1/reject") {
                contentType(ContentType.Application.Json)
                setBody(ReviewServiceRequest(reason = "Not appropriate"))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        } finally {
            cleanupService("test-reject-1")
        }
    }

    @Test
    fun `POST merge migrates service`() = integrationTestApplication {
        val token = obtainAuthToken("+5511999000004")

        createTestClient().get("/services/catalog")

        insertPendingService("test-merge-source", "Serviço para Merge")

        try {
            val client = createTestClient(token)
            val response = client.post("/admin/catalog/test-merge-source/merge") {
                contentType(ContentType.Application.Json)
                setBody(ReviewServiceRequest(reason = "Duplicate of clean-house", mergeIntoServiceId = "clean-house"))
            }
            assertEquals(HttpStatusCode.OK, response.status)
        } finally {
            cleanupService("test-merge-source")
        }
    }
}
