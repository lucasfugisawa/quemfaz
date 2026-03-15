package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery

class MockSearchQueryInterpreter(
    private val catalogService: CatalogService,
) : SearchQueryInterpreter {
    override fun interpret(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery {
        val normalized = query.lowercase().trim()
        val serviceIds = mutableSetOf<String>()

        catalogService.getActiveServices().forEach { service ->
            val keywords = service.aliases + service.displayName.lowercase()
            if (keywords.any { normalized.contains(it) }) {
                serviceIds.add(service.id)
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
