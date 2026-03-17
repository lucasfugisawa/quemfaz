package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.SignalCaptureService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.llm.LlmAgentService
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class LlmSearchQueryInterpreterFallbackTest {

    private val mockCatalogService: CatalogService = mock()
    private val mockSignalCaptureService: SignalCaptureService = mock()

    private val electricianEntry = CatalogEntry(
        id = "maintenance-electrician",
        displayName = "Elétrica Residencial",
        description = "Instalação e manutenção elétrica",
        categoryId = "MAINTENANCE",
        aliases = listOf("eletricista", "elétrico"),
        status = CatalogServiceStatus.ACTIVE,
    )
    private val cleanEntry = CatalogEntry(
        id = "clean-house",
        displayName = "Limpeza Residencial",
        description = "Limpeza da casa",
        categoryId = "CLEANING",
        aliases = listOf("faxina", "diarista", "limpeza"),
        status = CatalogServiceStatus.ACTIVE,
    )
    private val paintEntry = CatalogEntry(
        id = "paint-residential",
        displayName = "Pintura Residencial",
        description = "Pintura de residências",
        categoryId = "PAINTING",
        aliases = listOf("pintor", "pintura"),
        status = CatalogServiceStatus.ACTIVE,
    )

    init {
        val entries = listOf(electricianEntry, cleanEntry, paintEntry)
        whenever(mockCatalogService.getActiveServices()).thenReturn(entries)
        // Mock search to simulate local matching behavior
        whenever(mockCatalogService.search("eletricista")).thenReturn(listOf(electricianEntry))
        whenever(mockCatalogService.search("pintor")).thenReturn(listOf(paintEntry))
        whenever(mockCatalogService.search("xyz abc 123")).thenReturn(emptyList())
    }

    private class FailingLlmAgentService : LlmAgentService(timeoutMs = 1000L) {
        override suspend fun <T> executeStructured(
            systemPrompt: String,
            userMessage: String,
            serializer: KSerializer<T>,
        ): T = throw RuntimeException("LLM unavailable")
    }

    private val interpreter = LlmSearchQueryInterpreter(
        FailingLlmAgentService(),
        mockCatalogService,
        mockSignalCaptureService,
    )

    @Test
    fun `fallback with matching alias returns matched service IDs`() {
        val result = interpreter.interpret("eletricista", "Batatais")

        assertTrue(result.serviceIds.isNotEmpty())
        assertTrue(result.serviceIds.contains("maintenance-electrician"))
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `fallback with no match returns empty service IDs`() {
        val result = interpreter.interpret("xyz abc 123", "Batatais")

        assertTrue(result.serviceIds.isEmpty())
        assertEquals("Batatais", result.cityName)
    }

    @Test
    fun `fallback preserves city context`() {
        val result = interpreter.interpret("pintor", "Franca")

        assertEquals("Franca", result.cityName)
    }
}
