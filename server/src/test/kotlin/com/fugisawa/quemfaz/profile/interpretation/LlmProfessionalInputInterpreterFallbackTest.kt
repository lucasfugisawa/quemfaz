package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class LlmProfessionalInputInterpreterFallbackTest {

    /** LlmAgentService that always throws to simulate LLM failure */
    private class FailingLlmAgentService : LlmAgentService(timeoutMs = 1000L) {
        override suspend fun <T> executeStructured(
            systemPrompt: String,
            userMessage: String,
            serializer: KSerializer<T>,
        ): T = throw RuntimeException("LLM unavailable")
    }

    private val interpreter = LlmProfessionalInputInterpreter(FailingLlmAgentService())

    @Test
    fun `fallback with matching alias sets llmUnavailable and returns matched services`() {
        val response = interpreter.interpret("Sou eletricista", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isNotEmpty())
        assertTrue(response.interpretedServices.any { it.serviceId == "repair-electrician" })
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `fallback with no matching alias sets llmUnavailable and returns empty services`() {
        val response = interpreter.interpret("xyz abc 123", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isEmpty())
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `fallback with empty services from LLM triggers local matching`() {
        val response = interpreter.interpret("pintor", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isNotEmpty())
        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" || it.serviceId == "paint-commercial" })
    }
}
