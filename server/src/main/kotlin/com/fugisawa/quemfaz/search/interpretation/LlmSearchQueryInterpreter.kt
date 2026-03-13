package com.fugisawa.quemfaz.search.interpretation

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
        val normalized = interpretation.service.lowercase()
        val serviceIds =
            CanonicalServices.all
                .filter { canonical ->
                    val keywords = canonical.baseAliases + canonical.displayName.lowercase()
                    keywords.any { normalized.contains(it) || it.contains(normalized) }
                }.map { it.id.value }

        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = query.lowercase().trim(),
            serviceIds = serviceIds,
            cityName = interpretation.city ?: cityContext,
            neighborhoods = interpretation.neighborhoods,
            freeTextAliases = listOf(interpretation.service),
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
            neighborhoods = emptyList(),
            freeTextAliases = emptyList(),
        )

    companion object {
        private val SYSTEM_PROMPT =
            """
            Extract structured search information from the user query.

            Rules:
            - identify the requested service
            - extract city if present
            - extract neighborhoods if present
            - do not invent information
            """.trimIndent()
    }
}
