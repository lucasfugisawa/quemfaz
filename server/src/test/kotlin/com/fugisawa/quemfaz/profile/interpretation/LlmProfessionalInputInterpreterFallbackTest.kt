package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.application.SignalCaptureService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.llm.LlmAgentService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmProfessionalInputInterpreterFallbackTest {

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
    private val paintResidentialEntry = CatalogEntry(
        id = "paint-residential",
        displayName = "Pintura Residencial",
        description = "Pintura de casas e apartamentos",
        categoryId = "PAINTING",
        aliases = listOf("pintor", "pintura residencial"),
        status = CatalogServiceStatus.ACTIVE,
    )
    private val paintCommercialEntry = CatalogEntry(
        id = "paint-commercial",
        displayName = "Pintura Comercial",
        description = "Pintura de lojas e escritórios",
        categoryId = "PAINTING",
        aliases = listOf("pintor comercial", "pintura comercial"),
        status = CatalogServiceStatus.ACTIVE,
    )

    init {
        val entries = listOf(electricianEntry, paintResidentialEntry, paintCommercialEntry)
        whenever(mockCatalogService.getActiveServices()).thenReturn(entries)
        // Mock search to simulate local matching behavior
        whenever(mockCatalogService.search("Sou eletricista")).thenReturn(listOf(electricianEntry))
        whenever(mockCatalogService.findById("maintenance-electrician")).thenReturn(electricianEntry)
        whenever(mockCatalogService.search("xyz abc 123")).thenReturn(emptyList())
        whenever(mockCatalogService.search("pintor")).thenReturn(listOf(paintResidentialEntry, paintCommercialEntry))
    }

    /** LlmAgentService that always throws to simulate LLM failure */
    private class FailingLlmAgentService : LlmAgentService(timeoutMs = 1000L) {
        override suspend fun <T> executeStructured(
            systemPrompt: String,
            userMessage: String,
            serializer: KSerializer<T>,
        ): T = throw RuntimeException("LLM unavailable")
    }

    private val interpreter = LlmProfessionalInputInterpreter(
        FailingLlmAgentService(),
        mockCatalogService,
        mockSignalCaptureService,
    )

    @Test
    fun `fallback with matching alias sets llmUnavailable and returns matched services`() {
        val response = interpreter.interpret("Sou eletricista", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isNotEmpty())
        assertTrue(response.interpretedServices.any { it.serviceId == "maintenance-electrician" })
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

    @Test
    fun `fallback provisions service when no local matches found`() {
        val provisionedEntry = CatalogEntry(
            id = "camera-installation",
            displayName = "Instalação de Câmeras",
            description = "Instalação de câmeras de segurança",
            categoryId = "SECURITY",
            aliases = listOf("câmera", "cftv"),
            status = CatalogServiceStatus.PENDING_REVIEW,
        )

        val mockSignal: SignalCaptureService = mock()
        val mockCatalog: CatalogService = mock()
        whenever(mockCatalog.getActiveServices()).thenReturn(emptyList())
        whenever(mockCatalog.search("Instalo câmeras de segurança")).thenReturn(emptyList())
        whenever(mockCatalog.findById("camera-installation")).thenReturn(provisionedEntry)
        runBlocking {
            whenever(
                mockSignal.captureSignal(
                    eq("Instalo câmeras de segurança"), eq("onboarding"), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), eq(true),
                )
            ).thenReturn("camera-installation")
        }

        val fallbackInterpreter = LlmProfessionalInputInterpreter(
            FailingLlmAgentService(),
            mockCatalog,
            mockSignal,
        )

        val response = fallbackInterpreter.interpret("Instalo câmeras de segurança", InputMode.TEXT)

        assertTrue(response.llmUnavailable)
        assertTrue(response.interpretedServices.isNotEmpty())
        assertEquals("camera-installation", response.interpretedServices.first().serviceId)
        assertEquals("pending_review", response.interpretedServices.first().status)
        assertTrue(response.missingFields.isEmpty())
    }
}
