package com.fugisawa.quemfaz.integration.catalog

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.CanonicalServicesTable
import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.config.ConfigLoader
import com.fugisawa.quemfaz.contract.catalog.PendingServiceResponse
import com.fugisawa.quemfaz.contract.catalog.ReviewServiceRequest
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfileServicesTable
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminCatalogIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = emptyList()

    companion object {
        private const val ADMIN_USER_ID = "test-admin-user"
        private const val NON_ADMIN_USER_ID = "test-regular-user"
        private const val JWT_SECRET = "test-secret"
        private const val JWT_ISSUER = "test-issuer"
        private const val JWT_AUDIENCE = "test-audience"
    }

    private fun createJwt(userId: String): String =
        JWT.create()
            .withAudience(JWT_AUDIENCE)
            .withIssuer(JWT_ISSUER)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(Algorithm.HMAC256(JWT_SECRET))

    private fun adminTestApplication(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) {
        val testConfig = MapApplicationConfig(
            "jwt.secret" to JWT_SECRET,
            "jwt.issuer" to JWT_ISSUER,
            "jwt.audience" to JWT_AUDIENCE,
            "jwt.expiresInMs" to "3600000",
            "db.host" to databaseConfig.host,
            "db.port" to databaseConfig.port.toString(),
            "db.name" to databaseConfig.name,
            "db.user" to databaseConfig.user,
            "db.pass" to databaseConfig.pass,
            "sms.provider" to "FAKE",
            "admin.userIds.size" to "1",
            "admin.userIds.0" to ADMIN_USER_ID,
        )
        integrationTestApplication(
            koinModules = listOf(
                module { single { testConfig } },
                module { single<AppConfig> { ConfigLoader.loadConfig(testConfig) } },
                com.fugisawa.quemfaz.config.infrastructureModule,
            ),
            block = block,
        )
    }

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
    fun `non-admin user gets 403 on admin endpoints`() = adminTestApplication {
        val token = createJwt(NON_ADMIN_USER_ID)

        // Warm up
        createTestClient().get("/services/catalog")

        val client = createTestClient(token)
        val response = client.get("/admin/catalog/pending")
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `GET pending returns pending services`() = adminTestApplication {
        val token = createJwt(ADMIN_USER_ID)

        // Warm up
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
    fun `POST approve changes service to active`() = adminTestApplication {
        val token = createJwt(ADMIN_USER_ID)

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
    fun `POST reject changes service to rejected`() = adminTestApplication {
        val token = createJwt(ADMIN_USER_ID)

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
    fun `POST merge migrates service and profiles`() = adminTestApplication {
        val token = createJwt(ADMIN_USER_ID)

        createTestClient().get("/services/catalog")

        insertPendingService("test-merge-source", "Serviço para Merge")

        // Insert a user + profile + service link using raw SQL to handle PG enum types and FKs
        transaction {
            exec(
                """
                INSERT INTO users (id, first_name, last_name, created_at, updated_at)
                VALUES ('test-user-merge', 'Test', 'User', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """.trimIndent()
            )
            exec(
                """
                INSERT INTO professional_profiles (id, user_id, normalized_description, city_name, contact_phone, completeness, status, created_at, updated_at)
                VALUES ('test-profile-merge', 'test-user-merge', 'Test', 'São Paulo', '+5511999999999', 'INCOMPLETE', 'DRAFT', NOW(), NOW())
                """.trimIndent()
            )
            exec(
                """
                INSERT INTO professional_profile_services (professional_profile_id, service_id, match_level)
                VALUES ('test-profile-merge', 'test-merge-source', 'PRIMARY')
                """.trimIndent()
            )
        }

        try {
            val client = createTestClient(token)
            val response = client.post("/admin/catalog/test-merge-source/merge") {
                contentType(ContentType.Application.Json)
                setBody(ReviewServiceRequest(reason = "Duplicate of clean-house", mergeIntoServiceId = "clean-house"))
            }
            assertEquals(HttpStatusCode.OK, response.status)

            // Verify profile was migrated to target service
            val migratedLinks = transaction {
                ProfessionalProfileServicesTable
                    .select(ProfessionalProfileServicesTable.serviceId)
                    .where { ProfessionalProfileServicesTable.professionalProfileId eq "test-profile-merge" }
                    .map { it[ProfessionalProfileServicesTable.serviceId] }
            }
            assertEquals(listOf("clean-house"), migratedLinks)
        } finally {
            transaction {
                exec("DELETE FROM professional_profile_services WHERE professional_profile_id = 'test-profile-merge'")
                exec("DELETE FROM professional_profiles WHERE id = 'test-profile-merge'")
                exec("DELETE FROM users WHERE id = 'test-user-merge'")
            }
            cleanupService("test-merge-source")
        }
    }
}
