package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.*
import com.fugisawa.quemfaz.llm.LlmAgentService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Instant

@Serializable
data class CandidateServiceDefinition(
    val serviceId: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
    val matchesExistingPendingId: String? = null,
)

class ProvisionalServiceCreator(
    private val catalogRepository: CatalogRepository,
    private val signalRepository: SignalRepository,
    private val catalogService: CatalogService,
    private val llmAgentService: LlmAgentService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun tryProvision(
        rawDescription: String,
        source: String,
        userId: String?,
        cityName: String?,
        safetyClassification: String?,
        safetyReason: String?,
    ): String? {
        if (!catalogService.isAutoProvisioningEnabled()) return null
        if (safetyClassification == "unsafe") return null

        return try {
            val candidate = generateCandidate(rawDescription)

            // Deduplication: check if LLM matched an existing pending service
            if (candidate.matchesExistingPendingId != null) {
                val existing = catalogRepository.findServiceById(candidate.matchesExistingPendingId)
                if (existing != null && existing.status == CatalogServiceStatus.PENDING_REVIEW) {
                    return existing.id
                }
            }

            // Deduplication: check exact ID match
            val existingById = catalogRepository.findServiceById(candidate.serviceId)
            if (existingById != null) {
                if (existingById.status == CatalogServiceStatus.PENDING_REVIEW) {
                    return existingById.id
                }
                return null
            }

            // Deduplication: check display name
            val pendingServices = catalogRepository.findServicesByStatus(CatalogServiceStatus.PENDING_REVIEW)
            val nameMatch = pendingServices.find {
                it.displayName.equals(candidate.displayName, ignoreCase = true)
            }
            if (nameMatch != null) return nameMatch.id

            // Validate category exists
            val categories = catalogService.getCategories()
            if (categories.none { it.id == candidate.categoryId }) {
                logger.warn("LLM suggested invalid category '{}' for provisional service", candidate.categoryId)
                return null
            }

            // Create the provisional service
            val service = CatalogServiceRecord(
                id = candidate.serviceId,
                displayName = candidate.displayName,
                description = candidate.description,
                categoryId = candidate.categoryId,
                aliases = candidate.aliases,
                status = CatalogServiceStatus.PENDING_REVIEW,
                createdBy = "system",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            catalogRepository.createService(service)
            catalogService.refreshCache()
            logger.info("Provisional service created: {} ({})", service.id, service.displayName)
            service.id
        } catch (e: Exception) {
            logger.error("Failed to create provisional service for '{}': {}", rawDescription, e.message)
            null
        }
    }

    private fun generateCandidate(rawDescription: String): CandidateServiceDefinition {
        val pendingServices = catalogRepository.findServicesByStatus(CatalogServiceStatus.PENDING_REVIEW)
        val pendingList = if (pendingServices.isNotEmpty()) {
            "\n\nExisting pending services (reuse if the description matches one of these):\n" +
                pendingServices.joinToString("\n") { "- ${it.id}: ${it.displayName}" }
        } else ""

        val activeList = catalogService.getActiveServices().joinToString("\n") { service ->
            "- ${service.id}: ${service.displayName} (aliases: ${service.aliases.joinToString(", ")})"
        }

        val prompt = """
            Generate a canonical service definition for the following user description.

            User description: "$rawDescription"

            Existing active services (do NOT duplicate these):
            $activeList
            $pendingList

            Rules:
            - Follow naming conventions: Portuguese, title case, concise
            - Assign to the most appropriate existing category from: ${catalogService.getCategories().joinToString(", ") { it.id }}
            - Generate a URL-friendly lowercase slug as the serviceId (e.g., "camera-installation")
            - Include common Portuguese aliases
            - If this matches an existing pending service, set matchesExistingPendingId to that service's ID
            - Do NOT create a service that significantly overlaps with an existing active service
        """.trimIndent()

        return runBlocking {
            llmAgentService.executeStructured<CandidateServiceDefinition>(
                systemPrompt = prompt,
                userMessage = rawDescription,
            )
        }
    }
}
