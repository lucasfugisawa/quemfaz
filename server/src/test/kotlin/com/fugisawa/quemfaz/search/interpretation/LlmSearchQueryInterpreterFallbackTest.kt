package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.llm.LlmAgentService
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class LlmSearchQueryInterpreterFallbackTest {

    private class FailingLlmAgentService : LlmAgentService(timeoutMs = 1000L) {
        override suspend fun <T> executeStructured(
            systemPrompt: String,
            userMessage: String,
            serializer: KSerializer<T>,
        ): T = throw RuntimeException("LLM unavailable")
    }

    private val interpreter = LlmSearchQueryInterpreter(FailingLlmAgentService())

    @Test
    fun `fallback with matching alias returns matched service IDs`() {
        val result = interpreter.interpret("eletricista", "Batatais")

        assertTrue(result.serviceIds.isNotEmpty())
        assertTrue(result.serviceIds.contains("repair-electrician"))
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
