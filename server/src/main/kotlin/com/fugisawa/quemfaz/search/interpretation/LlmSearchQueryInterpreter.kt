package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.SearchInterpretation
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class LlmSearchQueryInterpreter(
    private val llmAgentService: LlmAgentService,
) : SearchQueryInterpreter {
    private val logger = LoggerFactory.getLogger(LlmSearchQueryInterpreter::class.java)

    override fun interpret(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery =
        try {
            val interpretation =
                runBlocking {
                    llmAgentService.executeStructured<SearchInterpretation>(
                        systemPrompt = SYSTEM_PROMPT,
                        userMessage = "Query:\n$query",
                    )
                }
            mapToResult(query, cityContext, interpretation)
        } catch (e: Exception) {
            logger.error("LLM search interpretation failed, falling back to basic response", e)
            fallbackResult(query, cityContext)
        }

    private fun mapToResult(
        query: String,
        cityContext: String?,
        interpretation: SearchInterpretation,
    ): InterpretedSearchQuery {
        val canonical = CanonicalServices.findById(CanonicalServiceId(interpretation.serviceId))
        val serviceIds = if (canonical != null) listOf(canonical.id.value) else emptyList()

        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = query.lowercase().trim(),
            serviceIds = serviceIds,
            cityName = cityContext,
            freeTextAliases = if (canonical != null) listOf(canonical.displayName) else emptyList(),
        )
    }

    private fun fallbackResult(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery =
        InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = query.lowercase().trim(),
            serviceIds = emptyList(),
            cityName = cityContext,
            freeTextAliases = emptyList(),
        )

    companion object {
        private val CANONICAL_SERVICES_CATALOG =
            CanonicalServices.all.joinToString("\n") { service ->
                "- ${service.id.value}: ${service.displayName} (aliases: ${(service.baseAliases).joinToString(", ")})"
            }

        private val SYSTEM_PROMPT =
            """
            Extract structured search information from the user query.

            You MUST map the requested service to the canonical services supported by the platform.
            Use ONLY the service ID values from the catalog below. Do not invent new service IDs.
            If the query mentions a service not in the catalog, map it to the closest match or to "other-general".

            Supported services catalog:
            $CANONICAL_SERVICES_CATALOG

            Rules:
            - identify the requested service and map it to a canonical service ID from the catalog
            - the "serviceId" field must contain only an ID value from the catalog
            - do not invent information
            """.trimIndent()
    }
}
