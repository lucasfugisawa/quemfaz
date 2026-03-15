package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class LlmProfessionalInputInterpreter(
    private val llmAgentService: LlmAgentService,
) : ProfessionalInputInterpreter {
    private val logger = LoggerFactory.getLogger(LlmProfessionalInputInterpreter::class.java)

    override fun interpret(
        inputText: String,
        inputMode: InputMode,
    ): CreateProfessionalProfileDraftResponse =
        try {
            val interpretation =
                runBlocking {
                    llmAgentService.executeStructured<OnboardingInterpretation>(
                        systemPrompt = SYSTEM_PROMPT,
                        userMessage = "Description:\n$inputText",
                    )
                }
            mapToResponse(inputText, interpretation)
        } catch (e: Exception) {
            logger.error("LLM interpretation failed, falling back to basic response", e)
            fallbackResponse(inputText)
        }

    private fun mapToResponse(
        inputText: String,
        interpretation: OnboardingInterpretation,
    ): CreateProfessionalProfileDraftResponse {
        val interpretedServices =
            interpretation.serviceIds
                .mapNotNull { serviceId ->
                    CanonicalServices.findById(CanonicalServiceId(serviceId))
                }.map { canonical ->
                    InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
                }.distinctBy { it.serviceId }

        // Edge case: LLM succeeded but returned empty services without requesting clarification
        // Try local matching as a second attempt
        val finalServices = if (interpretedServices.isEmpty() && !interpretation.needsClarification) {
            CanonicalServices.search(inputText).map { canonical ->
                InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
            }
        } else {
            interpretedServices
        }

        val missingFields = mutableListOf<String>()
        val followUpQuestions = mutableListOf<String>()

        if (finalServices.isEmpty() && !interpretation.needsClarification) {
            missingFields.add("services")
        }

        if (interpretation.needsClarification) {
            followUpQuestions.addAll(interpretation.clarificationQuestions.take(2))
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = finalServices,
            cityName = null,
            missingFields = missingFields,
            followUpQuestions = followUpQuestions,
            freeTextAliases = finalServices.map { it.displayName },
            llmUnavailable = false,
        )
    }

    private fun fallbackResponse(inputText: String): CreateProfessionalProfileDraftResponse {
        val localMatches = CanonicalServices.search(inputText)
        val interpretedServices = localMatches.map { canonical ->
            InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = interpretedServices,
            cityName = null,
            missingFields = if (interpretedServices.isEmpty()) listOf("services") else emptyList(),
            followUpQuestions = emptyList(),
            freeTextAliases = interpretedServices.map { it.displayName },
            llmUnavailable = true,
        )
    }

    override fun interpretWithClarifications(
        originalDescription: String,
        clarificationAnswers: List<ClarificationAnswer>,
        inputMode: InputMode,
    ): CreateProfessionalProfileDraftResponse =
        try {
            val answersText = clarificationAnswers.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
            val userMessage = "Original description:\n$originalDescription\n\nClarification answers:\n$answersText"
            val interpretation =
                runBlocking {
                    llmAgentService.executeStructured<OnboardingInterpretation>(
                        systemPrompt = SYSTEM_PROMPT,
                        userMessage = userMessage,
                    )
                }
            mapToResponse(originalDescription, interpretation)
        } catch (e: Exception) {
            logger.error("LLM clarification interpretation failed, falling back to basic response", e)
            fallbackResponse(originalDescription)
        }

    companion object {
        private val CANONICAL_SERVICES_CATALOG =
            CanonicalServices.all.joinToString("\n") { service ->
                "- ${service.id.value}: ${service.displayName} (aliases: ${(service.baseAliases).joinToString(", ")})"
            }

        private val SYSTEM_PROMPT =
            """
            Extract structured data about a professional service provider.

            Return structured output.

            You MUST map the described services to the canonical services supported by the platform.
            Use ONLY the service IDs from the catalog below. Do not invent new service IDs.
            If the description mentions a service not in the catalog, map it to the closest match or to "other-general".

            Supported services catalog:
            $CANONICAL_SERVICES_CATALOG

            Rules:
            - infer services from description and map them to the canonical service ID values above
            - the "serviceIds" field must contain only ID values from the catalog
            - if important information about services is missing:
              set needsClarification = true
              generate up to 2 clarificationQuestions
            - clarificationQuestions must be short and objective
            """.trimIndent()
    }
}
