package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockSearchQueryInterpreterTest {
    private val mockCatalogService: CatalogService = mock()

    init {
        whenever(mockCatalogService.getActiveServices()).thenReturn(
            listOf(
                CatalogEntry(
                    id = "clean-house",
                    displayName = "Limpeza Residencial",
                    description = "Limpeza da casa",
                    categoryId = "CLEANING",
                    aliases = listOf("faxina", "diarista", "limpeza residencial"),
                    status = CatalogServiceStatus.ACTIVE,
                ),
                CatalogEntry(
                    id = "maintenance-electrician",
                    displayName = "Elétrica Residencial",
                    description = "Instalação e manutenção elétrica",
                    categoryId = "MAINTENANCE",
                    aliases = listOf("eletricista", "elétrico"),
                    status = CatalogServiceStatus.ACTIVE,
                ),
                CatalogEntry(
                    id = "clean-land",
                    displayName = "Limpeza de Terreno",
                    description = "Limpeza de lotes e terrenos",
                    categoryId = "CLEANING",
                    aliases = listOf("limpar lote", "limpeza de terreno", "roçada"),
                    status = CatalogServiceStatus.ACTIVE,
                ),
                CatalogEntry(
                    id = "event-bartender",
                    displayName = "Barman",
                    description = "Bartender para eventos",
                    categoryId = "EVENTS",
                    aliases = listOf("barman", "bartender", "bar"),
                    status = CatalogServiceStatus.ACTIVE,
                ),
            ),
        )
    }

    private val interpreter = MockSearchQueryInterpreter(mockCatalogService)

    @Test
    fun `should interpret cleaning service`() {
        val result = interpreter.interpret("Preciso de uma faxina amanhã", "Batatais")

        assertEquals("preciso de uma faxina amanhã", result.normalizedQuery)
        assertTrue(result.serviceIds.contains("clean-house"))
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `should interpret electrician and city`() {
        val result = interpreter.interpret("Eletricista em Franca", "Batatais")

        assertTrue(result.serviceIds.contains("maintenance-electrician"))
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `should interpret land cleaning service`() {
        val result = interpreter.interpret("Limpar lote no Jardim Bandeirantes", "Batatais")

        assertTrue(result.serviceIds.contains("clean-land"))
    }

    @Test
    fun `should interpret bartender for events`() {
        val result = interpreter.interpret("Barman para festa", "Batatais")

        assertTrue(result.serviceIds.contains("event-bartender"))
    }
}
