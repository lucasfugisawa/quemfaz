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

        // Heuristic keyword matching
        CanonicalServices.all.forEach { service ->
            val keywords = service.baseAliases + service.displayName.lowercase()
            if (keywords.any { normalized.contains(it) }) {
                serviceIds.add(service.id.value)
            }
        }

        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = normalized,
            serviceIds = serviceIds.toList(),
            cityName = cityContext,
            freeTextAliases = emptyList(),
            llmUnavailable = false,
        )
    }
}
