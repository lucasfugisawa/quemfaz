package com.fugisawa.quemfaz.catalog.domain

import java.time.Instant

data class ServiceCategory(
    val id: String,
    val displayName: String,
    val sortOrder: Int,
)

enum class CatalogServiceStatus {
    ACTIVE,
    PENDING_REVIEW,
    REJECTED,
    MERGED;

    fun toDbValue(): String = name.lowercase()

    companion object {
        fun fromDbValue(value: String): CatalogServiceStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: error("Unknown catalog service status: '$value'")
    }
}

data class CatalogServiceRecord(
    val id: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
    val status: CatalogServiceStatus,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val mergedIntoServiceId: String? = null,
    val reviewStatusReason: String? = null,
    val reviewedAt: Instant? = null,
    val reviewedBy: String? = null,
)

data class UnmatchedServiceSignal(
    val id: String,
    val rawDescription: String,
    val source: String,
    val userId: String? = null,
    val bestMatchServiceId: String? = null,
    val bestMatchConfidence: String? = null,
    val provisionalServiceId: String? = null,
    val cityName: String? = null,
    val safetyClassification: String? = null,
    val safetyReason: String? = null,
    val createdAt: Instant,
)
