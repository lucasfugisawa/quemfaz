package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.contract.profile.ClarificationAnswer
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel

class MockProfessionalInputInterpreter(
    private val catalogService: CatalogService,
) : ProfessionalInputInterpreter {
    override fun interpret(
        inputText: String,
        inputMode: InputMode,
    ): CreateProfessionalProfileDraftResponse {
        val normalizedText = inputText.lowercase()

        val interpretedServices =
            catalogService.getActiveServices()
                .flatMap { service ->
                    val matches = mutableListOf<InterpretedServiceDto>()
                    if (normalizedText.contains(service.displayName.lowercase())) {
                        matches.add(InterpretedServiceDto(service.id, service.displayName, ServiceMatchLevel.PRIMARY.name))
                    } else {
                        val matchingAlias = service.aliases.find { normalizedText.contains(it.lowercase()) }
                        if (matchingAlias != null) {
                            matches.add(InterpretedServiceDto(service.id, service.displayName, ServiceMatchLevel.PRIMARY.name))
                        }
                    }
                    matches
                }.distinctBy { it.serviceId }

        val missingFields = mutableListOf<String>()
        val followUpQuestions = mutableListOf<String>()

        if (interpretedServices.isEmpty()) {
            missingFields.add("services")
            followUpQuestions.add("Pode nos contar um pouco mais sobre os serviços que você oferece?")
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = interpretedServices,
            cityName = null,
            missingFields = missingFields,
            followUpQuestions = followUpQuestions.take(2),
            freeTextAliases = interpretedServices.map { it.displayName },
            llmUnavailable = false,
        )
    }

    override fun interpretWithClarifications(
        originalDescription: String,
        clarificationAnswers: List<ClarificationAnswer>,
        inputMode: InputMode,
    ): CreateProfessionalProfileDraftResponse {
        val combinedText = originalDescription + " " + clarificationAnswers.joinToString(" ") { it.answer }
        return interpret(combinedText, inputMode)
    }
}
