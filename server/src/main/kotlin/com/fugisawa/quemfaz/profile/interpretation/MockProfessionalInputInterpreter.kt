package com.fugisawa.quemfaz.profile.interpretation

import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel

class MockProfessionalInputInterpreter : ProfessionalInputInterpreter {
    override fun interpret(
        inputText: String,
        inputMode: InputMode,
    ): CreateProfessionalProfileDraftResponse {
        val normalizedText = inputText.lowercase()

        val interpretedServices =
            CanonicalServices.all
                .flatMap { service ->
                    val matches = mutableListOf<InterpretedServiceDto>()
                    if (normalizedText.contains(service.displayName.lowercase())) {
                        matches.add(InterpretedServiceDto(service.id.value, service.displayName, ServiceMatchLevel.PRIMARY.name))
                    } else {
                        val matchingAlias = service.baseAliases.find { normalizedText.contains(it.lowercase()) }
                        if (matchingAlias != null) {
                            matches.add(InterpretedServiceDto(service.id.value, service.displayName, ServiceMatchLevel.PRIMARY.name))
                        }
                    }
                    matches
                }.distinctBy { it.serviceId }

        val cities = listOf("Batatais", "Franca", "Ribeirão Preto")
        val cityName = cities.find { normalizedText.contains(it.lowercase()) }

        val neighborhoods = mutableListOf<String>()
        if (cityName == "Batatais") {
            val batataisNeighborhoods = listOf("Centro", "Jardim Bandeirantes", "Vila Maria", "Castelo")
            neighborhoods.addAll(batataisNeighborhoods.filter { normalizedText.contains(it.lowercase()) })
        }

        val missingFields = mutableListOf<String>()
        val followUpQuestions = mutableListOf<String>()

        if (interpretedServices.isEmpty()) {
            missingFields.add("services")
            followUpQuestions.add("Pode nos contar um pouco mais sobre os serviços que você oferece?")
        }

        if (cityName == null) {
            missingFields.add("city")
            followUpQuestions.add("Em qual cidade você atende?")
        } else if (neighborhoods.isEmpty()) {
            missingFields.add("neighborhoods")
            followUpQuestions.add("Quais bairros você atende em $cityName?")
        }

        return CreateProfessionalProfileDraftResponse(
            normalizedDescription = inputText.replaceFirstChar { it.uppercase() },
            interpretedServices = interpretedServices,
            cityName = cityName,
            neighborhoods = neighborhoods,
            missingFields = missingFields,
            followUpQuestions = followUpQuestions.take(2),
            freeTextAliases = interpretedServices.map { it.displayName },
        )
    }
}
