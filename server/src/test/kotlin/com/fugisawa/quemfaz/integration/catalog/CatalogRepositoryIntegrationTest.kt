package com.fugisawa.quemfaz.integration.catalog

import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.catalog.domain.UnmatchedServiceSignal
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.ExposedCatalogRepository
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.ExposedSignalRepository
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.ExposedSystemConfigRepository
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.UnmatchedServiceSignalsTable
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.request.get
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogRepositoryIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> =
        listOf(
            UnmatchedServiceSignalsTable,
            // Do NOT clean CanonicalServicesTable or ServiceCategoriesTable — they are seeded by migration
        )

    @Test
    fun `migration seeds 7 categories`() =
        integrationTestApplication {
            createTestClient().get("/") // warmup: ensures Application.module() runs and DB.connect() is called
            val repo = ExposedCatalogRepository()
            val categories = repo.findAllCategories()
            assertEquals(7, categories.size)
            assertTrue(categories.any { it.id == "CLEANING" })
            assertTrue(categories.none { it.id == "OTHER" })
        }

    @Test
    fun `migration seeds 22 active services`() =
        integrationTestApplication {
            createTestClient().get("/")
            val repo = ExposedCatalogRepository()
            val services = repo.findServicesByStatus(CatalogServiceStatus.ACTIVE)
            assertEquals(22, services.size)
            assertTrue(services.none { it.id == "other-general" })
        }

    @Test
    fun `findServiceById returns seeded service`() =
        integrationTestApplication {
            createTestClient().get("/")
            val repo = ExposedCatalogRepository()
            val service = repo.findServiceById("clean-house")
            assertNotNull(service)
            assertEquals("Limpeza Residencial", service.displayName)
            assertEquals("CLEANING", service.categoryId)
            assertEquals(CatalogServiceStatus.ACTIVE, service.status)
            assertTrue(service.aliases.contains("diarista"))
        }

    @Test
    fun `findServiceById returns null for nonexistent`() =
        integrationTestApplication {
            createTestClient().get("/")
            val repo = ExposedCatalogRepository()
            assertNull(repo.findServiceById("nonexistent"))
        }

    @Test
    fun `signal repository creates and retrieves by provisional service ID`() =
        integrationTestApplication {
            createTestClient().get("/")
            val catalogRepo = ExposedCatalogRepository()
            val signalRepo = ExposedSignalRepository()

            // Use a real seeded service as the provisional service ID (FK constraint)
            val provisionalId = "clean-house"

            val signal =
                UnmatchedServiceSignal(
                    id = UUID.randomUUID().toString(),
                    rawDescription = "instalo câmeras de segurança",
                    source = "onboarding",
                    userId = null,
                    bestMatchServiceId = "maintenance-electrician",
                    bestMatchConfidence = "low",
                    provisionalServiceId = provisionalId,
                    cityName = "São Paulo",
                    safetyClassification = "safe",
                    safetyReason = null,
                    createdAt = Instant.now(),
                )
            signalRepo.create(signal)

            // Positive case: retrieve by the actual provisional service ID
            val found = signalRepo.findByProvisionalServiceId(provisionalId)
            assertTrue(found.isNotEmpty())
            assertTrue(found.any { it.rawDescription == signal.rawDescription })
            assertEquals("onboarding", found.first { it.rawDescription == signal.rawDescription }.source)

            // Negative case: different provisional service ID returns empty
            val notFound = signalRepo.findByProvisionalServiceId("nonexistent-id")
            assertTrue(notFound.isEmpty())
        }

    @Test
    fun `system config repository reads default value`() =
        integrationTestApplication {
            createTestClient().get("/")
            val repo = ExposedSystemConfigRepository()
            val value = repo.get("catalog.auto-provisioning.enabled")
            assertEquals("false", value)
        }

    @Test
    fun `system config repository writes and reads`() =
        integrationTestApplication {
            createTestClient().get("/")
            val repo = ExposedSystemConfigRepository()
            repo.set("catalog.auto-provisioning.enabled", "true")
            assertEquals("true", repo.get("catalog.auto-provisioning.enabled"))
            // Reset
            repo.set("catalog.auto-provisioning.enabled", "false")
        }
}
