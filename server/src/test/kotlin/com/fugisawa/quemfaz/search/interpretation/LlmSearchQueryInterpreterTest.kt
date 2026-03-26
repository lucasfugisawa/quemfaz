package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.application.SignalCaptureService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.SearchInterpretation
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmSearchQueryInterpreterTest {
    private val mockCatalogService: CatalogService = mock()
    private val mockSignalCaptureService: SignalCaptureService = mock()

    private val paintEntry =
        CatalogEntry(
            id = "paint-residential",
            displayName = "Pintura Residencial",
            description = "Pintura de residências",
            categoryId = "PAINTING",
            aliases = listOf("pintor", "pintura"),
            status = CatalogServiceStatus.ACTIVE,
        )

    init {
        whenever(mockCatalogService.getActiveServices()).thenReturn(listOf(paintEntry))
        whenever(mockCatalogService.findById("paint-residential")).thenReturn(paintEntry)
        whenever(mockCatalogService.search(any())).thenReturn(emptyList())
    }

    private fun createFakeService(
        response: Any? = null,
        exception: Exception? = null,
    ): LlmAgentService {
        return object : LlmAgentService(mock(), 8000L) {
            @Suppress("UNCHECKED_CAST")
            override suspend fun <T> executeStructured(
                systemPrompt: String,
                userMessage: String,
                serializer: KSerializer<T>,
            ): T {
                exception?.let { throw it }
                return response as T
            }
        }
    }

    @Test
    fun `should interpret search query via LLM`() {
        val service =
            createFakeService(
                SearchInterpretation(
                    serviceIds = listOf("paint-residential"),
                ),
            )
        val interpreter = LlmSearchQueryInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val result = interpreter.interpret("pintor residencial em Batatais Centro", "Batatais")

        assertTrue(result.serviceIds.contains("paint-residential"))
        assertEquals("Batatais", result.cityId)
    }

    @Test
    fun `should use cityContext when LLM returns no city`() {
        val service =
            createFakeService(
                SearchInterpretation(
                    serviceIds = listOf("paint-residential"),
                ),
            )
        val interpreter = LlmSearchQueryInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val result = interpreter.interpret("pintor", "Franca")

        assertEquals("Franca", result.cityId)
    }

    @Test
    fun `should fallback gracefully on LLM failure`() {
        val service = createFakeService(exception = RuntimeException("API error"))
        val interpreter = LlmSearchQueryInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val result = interpreter.interpret("pintor em Batatais", "Batatais")

        assertTrue(result.llmUnavailable)
        assertEquals("Batatais", result.cityId)
    }
}
