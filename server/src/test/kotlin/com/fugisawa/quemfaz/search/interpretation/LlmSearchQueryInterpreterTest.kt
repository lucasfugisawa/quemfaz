package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.SearchInterpretation
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmSearchQueryInterpreterTest {
    private fun createFakeService(
        response: Any? = null,
        exception: Exception? = null,
    ): LlmAgentService {
        return object : LlmAgentService(mock()) {
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
                    service = "pintura residencial",
                    city = "Batatais",
                    neighborhoods = listOf("Centro"),
                ),
            )
        val interpreter = LlmSearchQueryInterpreter(service)

        val result = interpreter.interpret("pintor residencial em Batatais Centro", null)

        assertTrue(result.serviceIds.contains("paint-residential"))
        assertEquals("Batatais", result.cityName)
        assertEquals(listOf("Centro"), result.neighborhoods)
    }

    @Test
    fun `should use cityContext when LLM returns no city`() {
        val service =
            createFakeService(
                SearchInterpretation(
                    service = "pintura residencial",
                    city = null,
                    neighborhoods = emptyList(),
                ),
            )
        val interpreter = LlmSearchQueryInterpreter(service)

        val result = interpreter.interpret("pintor", "Franca")

        assertEquals("Franca", result.cityName)
    }

    @Test
    fun `should fallback gracefully on LLM failure`() {
        val service = createFakeService(exception = RuntimeException("API error"))
        val interpreter = LlmSearchQueryInterpreter(service)

        val result = interpreter.interpret("pintor em Batatais", "Batatais")

        assertTrue(result.serviceIds.isEmpty())
        assertEquals("Batatais", result.cityName)
        assertTrue(result.neighborhoods.isEmpty())
    }
}
