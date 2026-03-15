package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.*
import org.slf4j.LoggerFactory

class CatalogService(
    private val catalogRepository: CatalogRepository,
    private val configRepository: SystemConfigRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Volatile private var cachedCategories: List<ServiceCategory> = emptyList()
    @Volatile private var cachedActiveServices: List<CatalogEntry> = emptyList()
    @Volatile private var cachedAllVisibleServices: List<CatalogEntry> = emptyList()
    @Volatile private var byId: Map<String, CatalogEntry> = emptyMap()
    @Volatile private var catalogVersion: String = ""

    init {
        refreshCache()
    }

    fun refreshCache() {
        cachedCategories = catalogRepository.findAllCategories()
        val active = catalogRepository.findServicesByStatus(CatalogServiceStatus.ACTIVE)
        val pendingReview = catalogRepository.findServicesByStatus(CatalogServiceStatus.PENDING_REVIEW)
        cachedActiveServices = active.map { it.toEntry() }
        cachedAllVisibleServices = (active + pendingReview).map { it.toEntry() }
        byId = cachedAllVisibleServices.associateBy { it.id }
        catalogVersion = active.hashCode().toString(16)
        logger.info("Catalog cache refreshed: {} active, {} pending_review", active.size, pendingReview.size)
    }

    fun getCategories(): List<ServiceCategory> = cachedCategories

    fun getActiveServices(): List<CatalogEntry> = cachedActiveServices

    fun getAllVisibleServices(): List<CatalogEntry> = cachedAllVisibleServices

    fun findById(id: String): CatalogEntry? = byId[id]

    fun getCatalogVersion(): String = catalogVersion

    fun isAutoProvisioningEnabled(): Boolean =
        configRepository.get("catalog.auto-provisioning.enabled") == "true"

    /**
     * Weighted search across active services only (used for local matching fallback).
     * Same scoring algorithm as the original CanonicalServices.search().
     */
    fun search(query: String): List<CatalogEntry> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase().trim()
        val results = mutableListOf<Pair<CatalogEntry, Int>>()

        cachedActiveServices.forEach { service ->
            var score = 0
            val lowerDisplayName = service.displayName.lowercase()

            if (lowerDisplayName == lowerQuery) score += 100
            else if (lowerDisplayName.contains(lowerQuery)) score += 50
            else if (lowerQuery.contains(lowerDisplayName)) score += 70

            service.aliases.forEach { alias ->
                val lowerAlias = alias.lowercase()
                if (lowerAlias == lowerQuery) score += 80
                else if (lowerAlias.contains(lowerQuery)) score += 40
                else if (lowerQuery.contains(lowerAlias)) score += 60
            }

            if (service.description.lowercase().contains(lowerQuery)) score += 10

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
