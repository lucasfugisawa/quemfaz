package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.*
import org.slf4j.LoggerFactory
import java.text.Normalizer

private data class CatalogSnapshot(
    val categories: List<ServiceCategory>,
    val activeServices: List<CatalogEntry>,
    val allVisibleServices: List<CatalogEntry>,
    val byId: Map<String, CatalogEntry>,
    val version: String,
)

class CatalogService(
    private val catalogRepository: CatalogRepository,
    private val configRepository: SystemConfigRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var snapshot = CatalogSnapshot(
        categories = emptyList(),
        activeServices = emptyList(),
        allVisibleServices = emptyList(),
        byId = emptyMap(),
        version = "",
    )

    init {
        refreshCache()
    }

    fun refreshCache() {
        val categories = catalogRepository.findAllCategories()
        val active = catalogRepository.findServicesByStatus(CatalogServiceStatus.ACTIVE)
        val pendingReview = catalogRepository.findServicesByStatus(CatalogServiceStatus.PENDING_REVIEW)
        val activeEntries = active.map { it.toEntry() }
        val allVisible = (active + pendingReview).map { it.toEntry() }
        val version = configRepository.get("catalog.version") ?: "0"

        snapshot = CatalogSnapshot(
            categories = categories,
            activeServices = activeEntries,
            allVisibleServices = allVisible,
            byId = allVisible.associateBy { it.id },
            version = version,
        )
        logger.info("Catalog cache refreshed: {} active, {} pending_review, version={}", active.size, pendingReview.size, version)
    }

    fun getCategories(): List<ServiceCategory> = snapshot.categories

    fun getActiveServices(): List<CatalogEntry> = snapshot.activeServices

    fun getAllVisibleServices(): List<CatalogEntry> = snapshot.allVisibleServices

    fun findById(id: String): CatalogEntry? = snapshot.byId[id]

    fun getCatalogVersion(): String = snapshot.version

    fun isAutoProvisioningEnabled(): Boolean =
        configRepository.get("catalog.auto-provisioning.enabled") == "true"

    fun incrementVersion() {
        val current = configRepository.get("catalog.version")?.toLongOrNull() ?: 0
        configRepository.set("catalog.version", (current + 1).toString())
    }

    /**
     * Weighted search across active services only (used for local matching fallback).
     * Accent-insensitive matching for Portuguese text.
     */
    fun search(query: String): List<CatalogEntry> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.stripAccents().lowercase().trim()
        val results = mutableListOf<Pair<CatalogEntry, Int>>()

        snapshot.activeServices.forEach { service ->
            var score = 0
            val normalizedDisplayName = service.displayName.stripAccents().lowercase()

            if (normalizedDisplayName == normalizedQuery) score += 100
            else if (normalizedDisplayName.contains(normalizedQuery)) score += 50
            else if (normalizedQuery.contains(normalizedDisplayName)) score += 70

            service.aliases.forEach { alias ->
                val normalizedAlias = alias.stripAccents().lowercase()
                if (normalizedAlias == normalizedQuery) score += 80
                else if (normalizedAlias.contains(normalizedQuery)) score += 40
                else if (normalizedQuery.contains(normalizedAlias)) score += 60
            }

            if (service.description.stripAccents().lowercase().contains(normalizedQuery)) score += 10

            if (score > 0) results.add(service to score)
        }

        return results.sortedByDescending { it.second }.map { it.first }
    }
}

/**
 * Lightweight view of a CatalogService domain model for cache use.
 * Named with suffix to avoid collision with the CatalogService class.
 */
data class CatalogEntry(
    val id: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
    val status: CatalogServiceStatus,
)

private fun CatalogServiceRecord.toEntry() = CatalogEntry(
    id = this.id,
    displayName = this.displayName,
    description = this.description,
    categoryId = this.categoryId,
    aliases = this.aliases,
    status = this.status,
)

private fun String.stripAccents(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
