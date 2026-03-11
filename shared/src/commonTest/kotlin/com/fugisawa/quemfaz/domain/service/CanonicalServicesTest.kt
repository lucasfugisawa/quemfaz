package com.fugisawa.quemfaz.domain.service

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanonicalServicesTest {
    @Test
    fun `all services should have unique IDs`() {
        val ids = CanonicalServices.all.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals(ids.size, uniqueIds.size, "All service IDs should be unique")
    }

    @Test
    fun `all services should have non-empty display names`() {
        CanonicalServices.all.forEach { service ->
            assertTrue(service.displayName.isNotBlank(), "Service ${service.id} should have a display name")
        }
    }

    @Test
    fun `all services should have descriptions`() {
        CanonicalServices.all.forEach { service ->
            assertTrue(service.description.isNotBlank(), "Service ${service.id} should have a description")
        }
    }

    @Test
    fun `findById should find existing service`() {
        val service = CanonicalServices.findById(CanonicalServiceId("clean-house"))
        assertNotNull(service)
        assertEquals("Limpeza Residencial", service.displayName)
    }

    @Test
    fun `findById should return null for non-existing service`() {
        val service = CanonicalServices.findById(CanonicalServiceId("non-existing"))
        assertNull(service)
    }

    @Test
    fun `findByDisplayName should be case insensitive`() {
        val service1 = CanonicalServices.findByDisplayName("Limpeza Residencial")
        val service2 = CanonicalServices.findByDisplayName("limpeza residencial")
        val service3 = CanonicalServices.findByDisplayName("LIMPEZA RESIDENCIAL")

        assertNotNull(service1)
        assertNotNull(service2)
        assertNotNull(service3)
        assertEquals(service1, service2)
        assertEquals(service2, service3)
    }

    @Test
    fun `findByAlias should find services by alias`() {
        val services = CanonicalServices.findByAlias("diarista")
        assertTrue(services.isNotEmpty())
        assertTrue(services.any { it.displayName == "Limpeza Residencial" })
    }

    @Test
    fun `findByAlias should be case insensitive`() {
        val services1 = CanonicalServices.findByAlias("diarista")
        val services2 = CanonicalServices.findByAlias("DIARISTA")
        assertEquals(services1, services2)
    }

    @Test
    fun `findByAlias should return empty list for unknown alias`() {
        val services = CanonicalServices.findByAlias("unknown-alias-xyz")
        assertTrue(services.isEmpty())
    }

    @Test
    fun `findByCategory should return all services in category`() {
        val cleaningServices = CanonicalServices.findByCategory(ServiceCategory.CLEANING)
        assertTrue(cleaningServices.isNotEmpty())
        assertTrue(cleaningServices.all { it.category == ServiceCategory.CLEANING })
    }

    @Test
    fun `search should find services by display name`() {
        val results = CanonicalServices.search("Limpeza")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.displayName.contains("Limpeza", ignoreCase = true) })
    }

    @Test
    fun `search should find services by alias`() {
        val results = CanonicalServices.search("diarista")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.baseAliases.any { alias -> alias.contains("diarista", ignoreCase = true) } })
    }

    @Test
    fun `search should return empty list for blank query`() {
        val results = CanonicalServices.search("")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search should return results ordered by relevance`() {
        val results = CanonicalServices.search("pintura")
        assertTrue(results.isNotEmpty())
        // Exact display name match should come first
        val first = results.first()
        assertTrue(first.displayName.contains("Pintura", ignoreCase = true))
    }

    @Test
    fun `all categories should have at least one service`() {
        ServiceCategory.entries.forEach { category ->
            val services = CanonicalServices.findByCategory(category)
            assertTrue(services.isNotEmpty(), "Category $category should have at least one service")
        }
    }
}
