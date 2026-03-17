package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.application.SignalCaptureService
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.OnboardingInterpretation
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class LlmProfessionalInputInterpreter(
    private val llmAgentService: LlmAgentService,
    private val catalogService: CatalogService,
    private val signalCaptureService: SignalCaptureService,
) : ProfessionalInputInterpreter {
    private val logger = LoggerFactory.getLogger(LlmProfessionalInputInterpreter::class.java)

    override fun interpret(
        inputText: String,
        inputMode: InputMode,
    ): CreateProfessionalProfileDraftResponse =
        try {
            runBlocking {
                val interpretation =
                    llmAgentService.executeStructured<OnboardingInterpretation>(
                        systemPrompt = buildSystemPrompt(),
                        userMessage = "Description:\n$inputText",
                    )
                mapToResponse(inputText, interpretation)
            }
        } catch (e: Exception) {
            logger.error(
                "LLM interpretation failed [flow=onboarding, inputLength={}, errorType={}, message={}]. Engaging local matching fallback.",
                inputText.length,
                e::class.simpleName,
                e.message,
            )
            fallbackResponse(inputText)
        }

    private suspend fun mapToResponse(
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

        // Process unmatched descriptions — capture signals, provision safe services, collect blocked
        val blockedDescriptions = mutableListOf<String>()
        val provisionedServices = mutableListOf<InterpretedServiceDto>()
        interpretation.unmatchedDescriptions.forEach { unmatched ->
            val provisionalId = signalCaptureService.captureSignal(
                unmatched.rawDescription, "onboarding", null, null,
                unmatched.safetyClassification, unmatched.safetyReason,
                forceProvision = true,
            )
            if (unmatched.safetyClassification == "unsafe") {
                blockedDescriptions.add(unmatched.rawDescription)
            } else if (provisionalId != null) {
                val entry = catalogService.findById(provisionalId)
                if (entry != null) {
                    provisionedServices.add(
                        InterpretedServiceDto(
                            entry.id, entry.displayName, ServiceMatchLevel.PRIMARY.name,
                            status = "pending_review",
                        )
                    )
                }
            }
        }

        val allServices = (finalServices + provisionedServices).distinctBy { it.serviceId }

        val missingFields = mutableListOf<String>()
        val followUpQuestions = mutableListOf<String>()

        if (allServices.isEmpty() && !interpretation.needsClarification) {
            missingFields.add("services")
        }

        if (interpretation.needsClarification) {
            followUpQuestions.addAll(interpretation.clarificationQuestions.take(2))
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            editedDescription = interpretation.editedDescription.ifBlank {
                inputText.replaceFirstChar { it.uppercase() }
            },
            interpretedServices = allServices,
            cityName = null,
            missingFields = missingFields,
            followUpQuestions = followUpQuestions,
            freeTextAliases = allServices.map { it.displayName },
            llmUnavailable = false,
            blockedDescriptions = blockedDescriptions,
        )
    }

    private fun fallbackResponse(inputText: String): CreateProfessionalProfileDraftResponse {
        val localMatches = catalogService.search(inputText)
        val interpretedServices = localMatches.map { entry ->
            InterpretedServiceDto(entry.id, entry.displayName, ServiceMatchLevel.PRIMARY.name)
        }

        // Capture signal and try provisioning if no matches found
        val allServices = if (interpretedServices.isEmpty()) {
            val provisionalId = runBlocking {
                signalCaptureService.captureSignal(
                    inputText, "onboarding", null, null, null, null,
                    forceProvision = true,
                )
            }
            if (provisionalId != null) {
                val entry = catalogService.findById(provisionalId)
                if (entry != null) {
                    listOf(
                        InterpretedServiceDto(
                            entry.id, entry.displayName, ServiceMatchLevel.PRIMARY.name,
                            status = "pending_review",
                        )
                    )
                } else interpretedServices
            } else interpretedServices
        } else interpretedServices

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            editedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = allServices,
            cityName = null,
            missingFields = if (allServices.isEmpty()) listOf("services") else emptyList(),
            followUpQuestions = emptyList(),
            freeTextAliases = allServices.map { it.displayName },
            llmUnavailable = true,
        )
    }

    override fun interpretWithClarifications(
        originalDescription: String,
        clarificationAnswers: List<ClarificationAnswer>,
        inputMode: InputMode,
        clarificationRound: Int,
    ): CreateProfessionalProfileDraftResponse =
        try {
            runBlocking {
                val answersText = clarificationAnswers.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
                val userMessage = "Original description:\n$originalDescription\n\nClarification answers:\n$answersText"
                val systemPrompt = if (clarificationRound >= 1) {
                    buildSystemPrompt() + "\n\n" + """
                        IMPORTANT: This is a follow-up after clarification round $clarificationRound.
                        Do NOT request further clarification. You MUST set needsClarification = false.
                        Work with the information you have. Map what you can to the catalog and add
                        the rest to unmatchedDescriptions as "safe".
                    """.trimIndent()
                } else {
                    buildSystemPrompt()
                }
                val interpretation =
                    llmAgentService.executeStructured<OnboardingInterpretation>(
                        systemPrompt = systemPrompt,
                        userMessage = userMessage,
                    )
                // Server-side enforcement: after round 1, force needsClarification=false
                val finalInterpretation = if (clarificationRound >= 1) {
                    interpretation.copy(needsClarification = false, clarificationQuestions = emptyList())
                } else {
                    interpretation
                }
                mapToResponse(originalDescription, finalInterpretation)
            }
        } catch (e: Exception) {
            logger.error(
                "LLM interpretation failed [flow=onboarding, inputLength={}, errorType={}, message={}]. Engaging local matching fallback.",
                originalDescription.length,
                e::class.simpleName,
                e.message,
            )
            fallbackResponse(originalDescription)
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
            - Classify its safety: "safe" or "unsafe"
            - For "unsafe", provide a brief "safetyReason"

            Safety classification:
            - "safe": any legitimate economic activity, regardless of whether it requires licenses,
              certifications, or professional registration. This platform is a marketplace — it does
              not verify credentials. That is the provider's and client's responsibility.
            - "unsafe": ONLY activities that are clearly illegal under Brazilian law. Examples:
              drug trafficking, weapons sales, fencing stolen goods, sexual exploitation,
              money laundering, fraud schemes.

            Do NOT classify as "unsafe" merely because:
            - The service requires a license or registration (CREA, CRM, OAB, etc.)
            - The service involves regulated materials (gas, electricity)
            - The service involves food preparation (padeiro, confeiteiro, cozinheiro)
            - The service involves physical risk (construction, roofing, waterproofing)
            - You are unfamiliar with the term or regional slang

            When in doubt, classify as "safe". The platform prefers to accept legitimate services
            and let the admin review process handle edge cases.

            Supported services catalog:
            $catalog

            Rules:
            - infer services from description and map them to the canonical service ID values above
            - the "serviceIds" field must contain only ID values from the catalog
            - clarificationQuestions must be short and objective

            IMPORTANT — Clarification rules:
            Request clarification (needsClarification = true) ONLY when the professional's description
            is genuinely ambiguous — when you cannot determine what kind of work they do.

            Do NOT request clarification just because the described service is not in the catalog.
            If you understand what they do but there is no matching catalog entry, add it to
            unmatchedDescriptions with classification "safe" and set needsClarification = false.

            Examples of when to clarify:
            - "Eu faço de tudo" — too vague, clarify
            - "Trabalho com manutenção" — somewhat vague, clarify what kind

            Examples of when NOT to clarify:
            - "Sou padeiro" — clear profession, not in catalog — unmatched, safe, no clarification
            - "Instalo câmeras" — clear service, not in catalog — unmatched, safe, no clarification
            - "Faço calhas e rufos" — clear trade, not in catalog — unmatched, safe, no clarification

            Description editing:
            You must also produce an "editedDescription" field — a lightly edited version of the user's
            original text, suitable as a public profile description.

            Rules for editedDescription:
            - Fix punctuation and capitalization
            - Split run-on sentences for readability
            - Slightly reorganize phrasing for clarity
            - Apply light condensation if the user was verbose about something simple
            - Apply a light transformation from "describing what I do" tone toward "profile description" tone
            - Remove filler words and false starts (e.g. "tipo", "né", "aí", "então")

            You MUST NOT:
            - Add information the user did not mention
            - Invent experience, credentials, or marketing claims
            - Introduce services not present in the original text
            - Translate the user's language into canonical service names from the catalog above
            - Remove meaningful information
            - Change the meaning of what was said

            The editedDescription must preserve the user's authentic voice and wording.
            It is a cleaned-up, slightly condensed, better-structured version of what they wrote — not a rewrite.
        """.trimIndent()
    }
}
