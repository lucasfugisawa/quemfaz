package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
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
            interpretation.services
                .flatMap { serviceName ->
                    val normalized = serviceName.lowercase()
                    CanonicalServices.all
                        .filter { canonical ->
                            val keywords = canonical.baseAliases + canonical.displayName.lowercase()
                            keywords.any { normalized.contains(it) || it.contains(normalized) }
                        }.map { canonical ->
                            InterpretedServiceDto(canonical.id.value, canonical.displayName, ServiceMatchLevel.PRIMARY.name)
                        }
                }.distinctBy { it.serviceId }

        val missingFields = mutableListOf<String>()
        val followUpQuestions = mutableListOf<String>()

        if (interpretedServices.isEmpty()) {
            missingFields.add("services")
        }
        if (interpretation.city == null) {
            missingFields.add("city")
        }

        if (interpretation.needsClarification) {
            followUpQuestions.addAll(interpretation.clarificationQuestions.take(2))
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = interpretedServices,
            cityName = interpretation.city,
            neighborhoods = interpretation.neighborhoods,
            missingFields = missingFields,
            followUpQuestions = followUpQuestions,
            freeTextAliases = interpretation.services,
        )
    }

    private fun fallbackResponse(inputText: String): CreateProfessionalProfileDraftResponse =
        CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = emptyList(),
            cityName = null,
            neighborhoods = emptyList(),
            missingFields = listOf("services", "city"),
            followUpQuestions = listOf("Pode nos contar um pouco mais sobre os serviços que você oferece?", "Em qual cidade você atende?"),
            freeTextAliases = emptyList(),
        )

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
        private val SYSTEM_PROMPT =
            """
            Extract structured data about a professional service provider.

            Return structured output.

            Rules:
            - infer services from description
            - extract city if present
            - extract neighborhoods if present
            - if important information is missing:
              set needsClarification = true
              generate up to 2 clarificationQuestions
            - clarificationQuestions must be short and objective
            """.trimIndent()
    }
}
