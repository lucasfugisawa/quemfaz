package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.contract.profile.InputMode
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockProfessionalInputInterpreterTest {
    private val mockCatalogService: CatalogService = mock()

    init {
        whenever(mockCatalogService.getActiveServices()).thenReturn(
            listOf(
                CatalogEntry(
                    id = "paint-residential",
                    displayName = "Pintura Residencial",
                    description = "Pintura de residências",
                    categoryId = "CONSTRUCTION",
                    aliases = listOf("pintura residencial", "pintor"),
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
                    id = "clean-house",
                    displayName = "Limpeza Residencial",
                    description = "Limpeza da casa",
                    categoryId = "CLEANING",
                    aliases = listOf("faxina", "diarista", "limpeza residencial"),
                    status = CatalogServiceStatus.ACTIVE,
                ),
            )
        )
    }

    private val interpreter = MockProfessionalInputInterpreter(mockCatalogService)

    @Test
    fun `should interpret painting service`() {
        val input = "Faço pintura residencial em Batatais"
        val response = interpreter.interpret(input, InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertNull(response.cityName)
        assertTrue(response.missingFields.isEmpty())
    }

    @Test
    fun `should interpret land cleaning service`() {
        val input = "Limpeza de terreno no Centro e Jardim Bandeirantes de Batatais"
        val response = interpreter.interpret(input, InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "clean-land" })
        assertNull(response.cityName)
        assertTrue(response.missingFields.isEmpty())
    }

    @Test
    fun `should detect missing services`() {
        val input = "Trabalho em Franca"
        val response = interpreter.interpret(input, InputMode.TEXT)

        assertTrue(response.interpretedServices.isEmpty())
        assertNull(response.cityName)
        assertTrue(response.missingFields.contains("services"))
    }
}
