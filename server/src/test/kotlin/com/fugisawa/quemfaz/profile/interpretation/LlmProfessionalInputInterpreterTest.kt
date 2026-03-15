package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmProfessionalInputInterpreterTest {
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
    fun `should interpret onboarding description via LLM`() {
        val service =
            createFakeService(
                OnboardingInterpretation(
                    serviceIds = listOf("paint-residential"),
                    needsClarification = false,
                ),
            )
        val interpreter = LlmProfessionalInputInterpreter(service)

        val response = interpreter.interpret("Faço pintura residencial em Batatais no Centro", InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertTrue(response.followUpQuestions.isEmpty())
        assertTrue(response.missingFields.isEmpty())
    }

    @Test
    fun `should return clarification questions when LLM says needsClarification`() {
        val service =
            createFakeService(
                OnboardingInterpretation(
                    serviceIds = listOf("paint-residential"),
                    needsClarification = true,
                    clarificationQuestions = listOf("Que tipo de pintura você faz?", "Trabalha com texturas?"),
                ),
            )
        val interpreter = LlmProfessionalInputInterpreter(service)

        val response = interpreter.interpret("Faço pintura", InputMode.TEXT)

        assertTrue(response.followUpQuestions.isNotEmpty())
        assertEquals(2, response.followUpQuestions.size)
    }

    @Test
    fun `should handle clarification answers`() {
        val service =
            createFakeService(
                OnboardingInterpretation(
                    serviceIds = listOf("paint-residential"),
                    needsClarification = false,
                ),
            )
        val interpreter = LlmProfessionalInputInterpreter(service)

        val answers =
            listOf(
                ClarificationAnswer("Que tipo de pintura você faz?", "Residencial"),
            )
        val response = interpreter.interpretWithClarifications("Faço pintura", answers, InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `should fallback gracefully on LLM failure`() {
        val service = createFakeService(exception = RuntimeException("API key missing"))
        val interpreter = LlmProfessionalInputInterpreter(service)

        val response = interpreter.interpret("Faço pintura em Batatais", InputMode.TEXT)

        assertTrue(response.interpretedServices.isEmpty())
        assertTrue(response.missingFields.contains("services"))
        assertTrue(response.followUpQuestions.isNotEmpty())
    }
}
