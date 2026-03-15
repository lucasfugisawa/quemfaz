package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.SignalRepository
import com.fugisawa.quemfaz.catalog.domain.UnmatchedServiceSignal
import com.fugisawa.quemfaz.llm.LlmAgentService
import com.fugisawa.quemfaz.llm.SearchInterpretation
import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class LlmSearchQueryInterpreter(
    private val llmAgentService: LlmAgentService,
    private val catalogService: CatalogService,
    private val signalRepository: SignalRepository,
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
                        systemPrompt = buildSystemPrompt(),
                        userMessage = "Query:\n$query",
                    )
                }
            mapToResult(query, cityContext, interpretation)
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

    private fun mapToResult(
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
            captureSignal(unmatched.rawDescription, "search", null, cityContext, unmatched.safetyClassification, unmatched.safetyReason)
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
            captureSignal(query, "search", null, cityContext, null, null)
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
            Extract structured search information from the user query.

            You MUST map the requested service to the canonical services supported by the platform.
            Use ONLY the service ID values from the catalog below. Do not invent new service IDs.

            If the query mentions a service that does NOT exist in the catalog:
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
            - identify the requested service and map it to canonical service IDs from the catalog
            - the "serviceIds" field must contain only ID values from the catalog
            - do not invent information
        """.trimIndent()
    }
}
