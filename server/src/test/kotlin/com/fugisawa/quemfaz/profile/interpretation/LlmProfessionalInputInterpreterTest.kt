package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.application.SignalCaptureService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import com.fugisawa.quemfaz.llm.UnmatchedDescription
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmProfessionalInputInterpreterTest {
    private val mockCatalogService: CatalogService = mock()
    private val mockSignalCaptureService: SignalCaptureService = mock()

    private val paintEntry = CatalogEntry(
        id = "paint-residential",
        displayName = "Pintura Residencial",
        description = "Pintura de residências",
        categoryId = "PAINTING",
        aliases = listOf("pintor", "pintura"),
        status = CatalogServiceStatus.ACTIVE,
    )

    private val provisionedBakeryEntry = CatalogEntry(
        id = "bakery-services",
        displayName = "Padaria",
        description = "Serviços de padaria",
        categoryId = "FOOD",
        aliases = listOf("padeiro"),
        status = CatalogServiceStatus.PENDING_REVIEW,
    )

    init {
        whenever(mockCatalogService.getActiveServices()).thenReturn(listOf(paintEntry))
        whenever(mockCatalogService.findById("paint-residential")).thenReturn(paintEntry)
        whenever(mockCatalogService.findById("bakery-services")).thenReturn(provisionedBakeryEntry)
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
    fun `should interpret onboarding description via LLM`() {
        val service =
            createFakeService(
                OnboardingInterpretation(
                    serviceIds = listOf("paint-residential"),
                    needsClarification = false,
                ),
            )
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

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
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

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
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val answers =
            listOf(
                ClarificationAnswer("Que tipo de pintura você faz?", "Residencial"),
            )
        val response = interpreter.interpretWithClarifications("Faço pintura", answers, InputMode.TEXT, clarificationRound = 1)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `should force no clarification on round 1 or higher`() {
        val service =
            createFakeService(
                OnboardingInterpretation(
                    serviceIds = emptyList(),
                    needsClarification = true,
                    clarificationQuestions = listOf("More detail?"),
                ),
            )
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val answers = listOf(ClarificationAnswer("What do you do?", "Sou padeiro"))
        val response = interpreter.interpretWithClarifications("Sou padeiro", answers, InputMode.TEXT, clarificationRound = 1)

        assertTrue(response.followUpQuestions.isEmpty(), "Should have no follow-up questions after round 1")
    }

    @Test
    fun `should fallback gracefully on LLM failure`() {
        val service = createFakeService(exception = RuntimeException("API key missing"))
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val response = interpreter.interpret("Faço pintura em Batatais", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.followUpQuestions.isEmpty())
    }

    @Test
    fun `should include provisioned services for safe unmatched descriptions`() {
        runBlocking {
            whenever(
                mockSignalCaptureService.captureSignal(
                    eq("Sou padeiro"), eq("onboarding"), anyOrNull(), anyOrNull(),
                    eq("safe"), anyOrNull(), eq(true),
                )
            ).thenReturn("bakery-services")
        }

        val service = createFakeService(
            OnboardingInterpretation(
                serviceIds = listOf("paint-residential"),
                needsClarification = false,
                unmatchedDescriptions = listOf(
                    UnmatchedDescription("Sou padeiro", "safe"),
                ),
            ),
        )
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val response = interpreter.interpret("Faço pintura e sou padeiro", InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertTrue(response.interpretedServices.any { it.serviceId == "bakery-services" })
        val bakery = response.interpretedServices.first { it.serviceId == "bakery-services" }
        assertEquals("pending_review", bakery.status)
    }

    @Test
    fun `should not provision unsafe unmatched descriptions`() {
        val service = createFakeService(
            OnboardingInterpretation(
                serviceIds = listOf("paint-residential"),
                needsClarification = false,
                unmatchedDescriptions = listOf(
                    UnmatchedDescription("Tráfico de drogas", "unsafe", "Atividade ilegal"),
                ),
            ),
        )
        val interpreter = LlmProfessionalInputInterpreter(service, mockCatalogService, mockSignalCaptureService)

        val response = interpreter.interpret("Faço pintura e tráfico", InputMode.TEXT)

        assertTrue(response.interpretedServices.any { it.serviceId == "paint-residential" })
        assertTrue(response.interpretedServices.none { it.status == "pending_review" })
        assertTrue(response.blockedDescriptions.contains("Tráfico de drogas"))
    }
}
