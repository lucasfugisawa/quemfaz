package com.fugisawa.quemfaz.city.application

import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.core.id.CityId
import com.fugisawa.quemfaz.domain.city.City
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CityServiceTest {

    private val batatais = City(
        id = CityId("batatais"),
        name = "Batatais",
        stateCode = "SP",
        country = "BR",
        latitude = -20.8914,
        longitude = -47.5864,
        isActive = true,
    )
    private val franca = City(
        id = CityId("franca"),
        name = "Franca",
        stateCode = "SP",
        country = "BR",
        latitude = -20.5389,
        longitude = -47.4008,
        isActive = true,
    )

    private fun createService(cities: List<City> = listOf(batatais, franca)): Pair<CityService, CountingCityRepository> {
        val repo = CountingCityRepository(cities)
        return CityService(repo) to repo
    }

    @Test
    fun `listActive returns all active cities`() {
        val (service, _) = createService()
        val response = service.listActive()
        assertEquals(2, response.cities.size)
        assertEquals("Batatais", response.cities.first { it.id == "batatais" }.name)
    }

    @Test
    fun `findById returns city from cache`() {
        val (service, repo) = createService()
        val city = service.findById("batatais")
        assertNotNull(city)
        assertEquals("Batatais", city.name)

        // Second call should use cache — repo only called once
        service.findById("batatais")
        assertEquals(1, repo.listActiveCallCount)
    }

    @Test
    fun `findById returns null for unknown id`() {
        val (service, _) = createService()
        assertNull(service.findById("unknown"))
    }

    @Test
    fun `resolveNameFromId returns name for valid id`() {
        val (service, _) = createService()
        assertEquals("Batatais", service.resolveNameFromId("batatais"))
    }

    @Test
    fun `resolveNameFromId returns null for null input`() {
        val (service, _) = createService()
        assertNull(service.resolveNameFromId(null))
    }

    @Test
    fun `resolveNameFromId returns null for unknown id`() {
        val (service, _) = createService()
        assertNull(service.resolveNameFromId("unknown"))
    }

    @Test
    fun `resolveIdFromName returns id for valid name`() {
        val (service, _) = createService()
        assertEquals("batatais", service.resolveIdFromName("Batatais"))
    }

    @Test
    fun `resolveIdFromName is case insensitive`() {
        val (service, _) = createService()
        assertEquals("batatais", service.resolveIdFromName("batatais"))
        assertEquals("batatais", service.resolveIdFromName("BATATAIS"))
    }

    @Test
    fun `resolveIdFromName returns null for null input`() {
        val (service, _) = createService()
        assertNull(service.resolveIdFromName(null))
    }

    @Test
    fun `resolveIdFromName returns null for unknown name`() {
        val (service, _) = createService()
        assertNull(service.resolveIdFromName("Unknown City"))
    }

    @Test
    fun `cache is hit on repeated calls — repository called only once`() {
        val (service, repo) = createService()
        service.findById("batatais")
        service.resolveNameFromId("franca")
        service.resolveIdFromName("Batatais")
        service.listActive()
        assertEquals(1, repo.listActiveCallCount)
    }

    private class CountingCityRepository(
        private val cities: List<City>,
    ) : CityRepository {
        var listActiveCallCount = 0
            private set

        override fun findById(id: String): City? = cities.find { it.id.value == id }
        override fun findByName(name: String): City? = cities.find { it.name.equals(name, ignoreCase = true) }
        override fun listActive(): List<City> {
            listActiveCallCount++
            return cities
        }
    }
}
