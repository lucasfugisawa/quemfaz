package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.SignalRepository
import com.fugisawa.quemfaz.catalog.domain.UnmatchedServiceSignal
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class LlmProfessionalInputInterpreter(
    private val llmAgentService: LlmAgentService,
    private val catalogService: CatalogService,
    private val signalRepository: SignalRepository,
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
                        systemPrompt = buildSystemPrompt(),
                        userMessage = "Description:\n$inputText",
                    )
                }
            mapToResponse(inputText, interpretation)
        } catch (e: Exception) {
            logger.error(
                "LLM interpretation failed [flow=onboarding, inputLength={}, errorType={}, message={}]. Engaging local matching fallback.",
                inputText.length,
                e::class.simpleName,
                e.message,
            )
            fallbackResponse(inputText)
        }

    private fun mapToResponse(
        inputText: String,
        interpretation: OnboardingInterpretation,
    ): CreateProfessionalProfileDraftResponse {
        val interpretedServices =
            interpretation.serviceIds
                .mapNotNull { serviceId ->
                    catalogService.findById(serviceId)
                }.map { entry ->
                    InterpretedServiceDto(entry.id, entry.displayName, ServiceMatchLevel.PRIMARY.name)
                }.distinctBy { it.serviceId }

        // Edge case: LLM succeeded but returned empty services without requesting clarification
        val finalServices = if (interpretedServices.isEmpty() && !interpretation.needsClarification) {
            catalogService.search(inputText).map { entry ->
                InterpretedServiceDto(entry.id, entry.displayName, ServiceMatchLevel.PRIMARY.name)
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

        // Process unmatched descriptions — capture signals, collect blocked
        val blockedDescriptions = mutableListOf<String>()
        interpretation.unmatchedDescriptions.forEach { unmatched ->
            captureSignal(unmatched.rawDescription, "onboarding", null, null, unmatched.safetyClassification, unmatched.safetyReason)
            if (unmatched.safetyClassification == "unsafe") {
                blockedDescriptions.add(unmatched.rawDescription)
            }
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = finalServices,
            cityName = null,
            missingFields = missingFields,
            followUpQuestions = followUpQuestions,
            freeTextAliases = finalServices.map { it.displayName },
            llmUnavailable = false,
            blockedDescriptions = blockedDescriptions,
        )
    }

    private fun fallbackResponse(inputText: String): CreateProfessionalProfileDraftResponse {
        val localMatches = catalogService.search(inputText)
        val interpretedServices = localMatches.map { entry ->
            InterpretedServiceDto(entry.id, entry.displayName, ServiceMatchLevel.PRIMARY.name)
        }

        // Capture signal if no matches found
        if (interpretedServices.isEmpty()) {
            captureSignal(inputText, "onboarding", null, null, null, null)
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
                        systemPrompt = buildSystemPrompt(),
                        userMessage = userMessage,
                    )
                }
            mapToResponse(originalDescription, interpretation)
        } catch (e: Exception) {
            logger.error(
                "LLM interpretation failed [flow=onboarding, inputLength={}, errorType={}, message={}]. Engaging local matching fallback.",
                originalDescription.length,
                e::class.simpleName,
                e.message,
            )
            fallbackResponse(originalDescription)
        }

    private fun captureSignal(
        rawDescription: String,
        source: String,
        userId: String?,
        cityName: String?,
        safetyClassification: String?,
        safetyReason: String?,
    ) {
        try {
            val bestMatch = catalogService.search(rawDescription).firstOrNull()
            signalRepository.create(
                UnmatchedServiceSignal(
                    id = UUID.randomUUID().toString(),
                    rawDescription = rawDescription,
                    source = source,
                    userId = userId,
                    bestMatchServiceId = bestMatch?.id,
                    bestMatchConfidence = if (bestMatch != null) "low" else "none",
                    provisionalServiceId = null,
                    cityName = cityName,
                    safetyClassification = safetyClassification,
                    safetyReason = safetyReason,
                    createdAt = Instant.now(),
                ),
            )
        } catch (e: Exception) {
            logger.error("Failed to capture unmatched service signal: {}", e.message)
        }
    }

    private fun buildSystemPrompt(): String {
        val catalog = catalogService.getActiveServices().joinToString("\n") { service ->
            "- ${service.id}: ${service.displayName} (aliases: ${service.aliases.joinToString(", ")})"
        }
        return """
            Extract structured data about a professional service provider.

            Return structured output.

            You MUST map the described services to the canonical services supported by the platform.
            Use ONLY the service IDs from the catalog below. Do not invent new service IDs.

            If the description mentions a service that does NOT exist in the catalog:
            - DO NOT force-map it to an unrelated service
            - Instead, add it to the "unmatchedDescriptions" array with the raw description
            - Classify its safety: "safe", "unsafe", or "uncertain"
            - For "unsafe" or "uncertain", provide a brief "safetyReason"

            A service is "unsafe" if it involves illegal activities, legally regulated services
            the platform cannot verify, or anything that would expose the platform to legal
            or reputational risk.

            Supported services catalog:
            $catalog

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
