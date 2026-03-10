package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery

class MockSearchQueryInterpreter : SearchQueryInterpreter {
    override fun interpret(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery {
        val normalized = query.lowercase().trim()
        val serviceIds = mutableSetOf<String>()
        val detectedNeighborhoods = mutableListOf<String>()
        var detectedCity = cityContext

        // Heuristic keyword matching
        CanonicalServices.all.forEach { service ->
            val keywords = service.baseAliases + service.displayName.lowercase()
            if (keywords.any { normalized.contains(it) }) {
                serviceIds.add(service.id.value)
            }
        }

        // Neighborhood heuristics
        val neighborhoods = listOf("Centro", "Jardim Bandeirantes", "Vila Nova", "Jardim América")
        neighborhoods.forEach { n ->
            if (normalized.contains(n.lowercase())) {
                detectedNeighborhoods.add(n)
            }
        }

        // City detection
        val cities = listOf("Batatais", "Franca", "Ribeirão Preto")
        cities.forEach { c ->
            if (normalized.contains(c.lowercase())) {
                detectedCity = c
            }
        }

        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = normalized,
            serviceIds = serviceIds.toList(),
            cityName = detectedCity,
            neighborhoods = detectedNeighborhoods,
            freeTextAliases = emptyList(), // Not used in Mock for now
        )
    }
}
