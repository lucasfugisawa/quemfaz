package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.application.SignalCaptureService
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.SearchInterpretation
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class LlmSearchQueryInterpreter(
    private val llmAgentService: LlmAgentService,
    private val catalogService: CatalogService,
    private val signalCaptureService: SignalCaptureService,
) : SearchQueryInterpreter {
    private val logger = LoggerFactory.getLogger(LlmSearchQueryInterpreter::class.java)

    override fun interpret(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery =
        try {
            runBlocking {
                val interpretation =
                    llmAgentService.executeStructured<SearchInterpretation>(
                        systemPrompt = buildSystemPrompt(),
                        userMessage = "Query:\n$query",
                    )
                mapToResult(query, cityContext, interpretation)
            }
        } catch (e: Exception) {
            logger.error(
                "LLM interpretation failed [flow=search, query={}, cityContext={}, errorType={}, message={}]. Engaging local matching fallback.",
                query,
                cityContext,
                e::class.simpleName,
                e.message,
            )
            fallbackResult(query, cityContext)
        }

    private suspend fun mapToResult(
        query: String,
        cityContext: String?,
        interpretation: SearchInterpretation,
    ): InterpretedSearchQuery {
        // Validate returned service IDs against catalog
        val validServices = interpretation.serviceIds
            .mapNotNull { id -> catalogService.findById(id)?.let { id to it } }

        // If LLM returned invalid IDs, try local matching
        val serviceIds = if (validServices.isEmpty() && interpretation.serviceIds.isNotEmpty()) {
            val localMatches = catalogService.search(query)
            localMatches.take(1).map { it.id }
        } else {
            validServices.map { it.first }
        }

        val displayNames = serviceIds.mapNotNull { catalogService.findById(it)?.displayName }

        // Process unmatched descriptions — capture signals, collect blocked
        val blockedDescriptions = mutableListOf<String>()
        interpretation.unmatchedDescriptions.forEach { unmatched ->
            signalCaptureService.captureSignal(
                unmatched.rawDescription, "search", null, cityContext,
                unmatched.safetyClassification, unmatched.safetyReason,
            )
            if (unmatched.safetyClassification == "unsafe") {
                blockedDescriptions.add(unmatched.rawDescription)
            }
        }

        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = query.lowercase().trim(),
            serviceIds = serviceIds,
            cityName = cityContext,
            freeTextAliases = displayNames,
            llmUnavailable = false,
            blockedDescriptions = blockedDescriptions,
        )
    }

    private fun fallbackResult(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery {
        val localMatches = catalogService.search(query)
        val serviceIds = localMatches.take(1).map { it.id }

        // Capture signal if no matches found
        if (serviceIds.isEmpty()) {
            runBlocking {
                signalCaptureService.captureSignal(query, "search", null, cityContext, null, null)
            }
        }

        return InterpretedSearchQuery(
            originalQuery = query,
            normalizedQuery = query.lowercase().trim(),
            serviceIds = serviceIds,
            cityName = cityContext,
            freeTextAliases = localMatches.take(1).map { it.displayName },
            llmUnavailable = true,
        )
    }

    private fun buildSystemPrompt(): String {
        val catalog = catalogService.getActiveServices().joinToString("\n") { service ->
            "- ${service.id}: ${service.displayName} (aliases: ${service.aliases.joinToString(", ")})"
        }
        return """
            Extract structured search information from the user query.

            You MUST map the requested service to the canonical services supported by the platform.
            Use ONLY the service ID values from the catalog below. Do not invent new service IDs.

            If the query mentions a service that does NOT exist in the catalog:
            - DO NOT force-map it to an unrelated service
            - Instead, add it to the "unmatchedDescriptions" array with the raw description
            - Classify its safety: "safe" or "unsafe"
            - For "unsafe", provide a brief "safetyReason"

            Safety classification:
            - "safe": any legitimate economic activity, regardless of whether it requires licenses,
              certifications, or professional registration. This platform is a marketplace — it does
              not verify credentials.
            - "unsafe": ONLY activities that are clearly illegal under Brazilian law. Examples:
              drug trafficking, weapons sales, fencing stolen goods, sexual exploitation,
              money laundering, fraud schemes.

            When in doubt, classify as "safe".

            Supported services catalog:
            $catalog

            Rules:
            - identify the requested service and map it to canonical service IDs from the catalog
            - the "serviceIds" field must contain only ID values from the catalog
            - do not invent information
        """.trimIndent()
    }
}
