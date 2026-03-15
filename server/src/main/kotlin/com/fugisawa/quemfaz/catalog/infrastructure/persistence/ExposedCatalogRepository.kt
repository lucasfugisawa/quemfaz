package com.fugisawa.quemfaz.catalog.infrastructure.persistence

import com.fugisawa.quemfaz.catalog.domain.*
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfileServicesTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

// --- Table Definitions ---

object ServiceCategoriesTable : Table("service_categories") {
    val id = text("id")
    val displayName = text("display_name")
    val sortOrder = integer("sort_order")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    override val primaryKey = PrimaryKey(id)
}

object CanonicalServicesTable : Table("canonical_services") {
    val id = text("id")
    val displayName = text("display_name")
    val description = text("description")
    val categoryId = text("category_id").references(ServiceCategoriesTable.id)
    val aliases = jsonb<List<String>>("aliases", Json)
    val status = text("status")
    val createdBy = text("created_by")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val mergedIntoServiceId = text("merged_into_service_id").nullable()
    val reviewStatusReason = text("review_status_reason").nullable()
    val reviewedAt = timestamp("reviewed_at").nullable()
    val reviewedBy = text("reviewed_by").nullable()
    override val primaryKey = PrimaryKey(id)
}

object UnmatchedServiceSignalsTable : Table("unmatched_service_signals") {
    val id = text("id")
    val rawDescription = text("raw_description")
    val signalSource = text("source")
    val userId = text("user_id").nullable()
    val bestMatchServiceId = text("best_match_service_id").nullable()
    val bestMatchConfidence = text("best_match_confidence").nullable()
    val provisionalServiceId = text("provisional_service_id").nullable()
    val cityName = text("city_name").nullable()
    val safetyClassification = text("safety_classification").nullable()
    val safetyReason = text("safety_reason").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    override val primaryKey = PrimaryKey(id)
}

object SystemConfigurationTable : Table("system_configuration") {
    val key = text("key")
    val value = text("value")
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    override val primaryKey = PrimaryKey(key)
}

// --- Mappers ---

private fun ResultRow.toServiceCategory() = ServiceCategory(
    id = this[ServiceCategoriesTable.id],
    displayName = this[ServiceCategoriesTable.displayName],
    sortOrder = this[ServiceCategoriesTable.sortOrder],
)

private fun ResultRow.toCatalogServiceRecord() = CatalogServiceRecord(
    id = this[CanonicalServicesTable.id],
    displayName = this[CanonicalServicesTable.displayName],
    description = this[CanonicalServicesTable.description],
    categoryId = this[CanonicalServicesTable.categoryId],
    aliases = this[CanonicalServicesTable.aliases],
    status = CatalogServiceStatus.fromDbValue(this[CanonicalServicesTable.status]),
    createdBy = this[CanonicalServicesTable.createdBy],
    createdAt = this[CanonicalServicesTable.createdAt],
    updatedAt = this[CanonicalServicesTable.updatedAt],
    mergedIntoServiceId = this[CanonicalServicesTable.mergedIntoServiceId],
    reviewStatusReason = this[CanonicalServicesTable.reviewStatusReason],
    reviewedAt = this[CanonicalServicesTable.reviewedAt],
    reviewedBy = this[CanonicalServicesTable.reviewedBy],
)

private fun ResultRow.toUnmatchedSignal() = UnmatchedServiceSignal(
    id = this[UnmatchedServiceSignalsTable.id],
    rawDescription = this[UnmatchedServiceSignalsTable.rawDescription],
    source = this[UnmatchedServiceSignalsTable.signalSource],
    userId = this[UnmatchedServiceSignalsTable.userId],
    bestMatchServiceId = this[UnmatchedServiceSignalsTable.bestMatchServiceId],
    bestMatchConfidence = this[UnmatchedServiceSignalsTable.bestMatchConfidence],
    provisionalServiceId = this[UnmatchedServiceSignalsTable.provisionalServiceId],
    cityName = this[UnmatchedServiceSignalsTable.cityName],
    safetyClassification = this[UnmatchedServiceSignalsTable.safetyClassification],
    safetyReason = this[UnmatchedServiceSignalsTable.safetyReason],
    createdAt = this[UnmatchedServiceSignalsTable.createdAt],
)

// --- Repository Implementations ---

class ExposedCatalogRepository : CatalogRepository {
    override fun findAllCategories(): List<ServiceCategory> = transaction {
        ServiceCategoriesTable
            .selectAll()
            .orderBy(ServiceCategoriesTable.sortOrder)
            .map { it.toServiceCategory() }
    }

    override fun findServicesByStatus(vararg statuses: CatalogServiceStatus): List<CatalogServiceRecord> = transaction {
        val statusValues = statuses.map { it.toDbValue() }
        CanonicalServicesTable
            .selectAll()
            .where { CanonicalServicesTable.status inList statusValues }
            .map { it.toCatalogServiceRecord() }
    }

    override fun findServiceById(id: String): CatalogServiceRecord? = transaction {
        CanonicalServicesTable
            .selectAll()
            .where { CanonicalServicesTable.id eq id }
            .firstOrNull()
            ?.toCatalogServiceRecord()
    }

    override fun createService(service: CatalogServiceRecord): CatalogServiceRecord = transaction {
        CanonicalServicesTable.insert {
            it[id] = service.id
            it[displayName] = service.displayName
            it[description] = service.description
            it[categoryId] = service.categoryId
            it[aliases] = service.aliases
            it[status] = service.status.toDbValue()
            it[createdBy] = service.createdBy
        }
        service
    }

    override fun updateServiceStatus(
        serviceId: String,
        newStatus: CatalogServiceStatus,
        reason: String?,
        reviewedBy: String?,
        mergedIntoServiceId: String?,
    ) = transaction {
        val now = Instant.now()
        CanonicalServicesTable.update({ CanonicalServicesTable.id eq serviceId }) {
            it[status] = newStatus.toDbValue()
            if (reason != null) it[reviewStatusReason] = reason
            if (reviewedBy != null) it[CanonicalServicesTable.reviewedBy] = reviewedBy
            it[reviewedAt] = now
            it[updatedAt] = now
            if (mergedIntoServiceId != null) {
                it[CanonicalServicesTable.mergedIntoServiceId] = mergedIntoServiceId
            }
        }
        Unit
    }

    override fun migrateProfileServices(fromServiceId: String, toServiceId: String) = transaction {
        // Collect the profile IDs that have fromServiceId so we can delete conflicts before migrating
        val affectedProfileIds = ProfessionalProfileServicesTable
            .selectAll()
            .where { ProfessionalProfileServicesTable.serviceId eq fromServiceId }
            .map { it[ProfessionalProfileServicesTable.professionalProfileId] }

        if (affectedProfileIds.isNotEmpty()) {
            // Delete any existing rows for the target service to avoid PK conflicts
            ProfessionalProfileServicesTable.deleteWhere {
                (ProfessionalProfileServicesTable.serviceId eq toServiceId) and
                    (ProfessionalProfileServicesTable.professionalProfileId inList affectedProfileIds)
            }
        }
        // Migrate remaining rows
        ProfessionalProfileServicesTable.update({ ProfessionalProfileServicesTable.serviceId eq fromServiceId }) {
            it[ProfessionalProfileServicesTable.serviceId] = toServiceId
        }
        Unit
    }

    override fun removeServiceFromProfiles(serviceId: String) = transaction {
        ProfessionalProfileServicesTable.deleteWhere {
            ProfessionalProfileServicesTable.serviceId eq serviceId
        }
        Unit
    }
}

class ExposedSignalRepository : SignalRepository {
    override fun create(signal: UnmatchedServiceSignal) = transaction {
        UnmatchedServiceSignalsTable.insert {
            it[id] = signal.id
            it[rawDescription] = signal.rawDescription
            it[signalSource] = signal.source
            it[userId] = signal.userId
            it[bestMatchServiceId] = signal.bestMatchServiceId
            it[bestMatchConfidence] = signal.bestMatchConfidence
            it[provisionalServiceId] = signal.provisionalServiceId
            it[cityName] = signal.cityName
            it[safetyClassification] = signal.safetyClassification
            it[safetyReason] = signal.safetyReason
        }
        Unit
    }

    override fun findByProvisionalServiceId(provisionalServiceId: String): List<UnmatchedServiceSignal> = transaction {
        UnmatchedServiceSignalsTable
            .selectAll()
            .where { UnmatchedServiceSignalsTable.provisionalServiceId eq provisionalServiceId }
            .orderBy(UnmatchedServiceSignalsTable.createdAt)
            .map { it.toUnmatchedSignal() }
    }
}

class ExposedSystemConfigRepository : SystemConfigRepository {
    override fun get(key: String): String? = transaction {
        SystemConfigurationTable
            .selectAll()
            .where { SystemConfigurationTable.key eq key }
            .firstOrNull()
            ?.get(SystemConfigurationTable.value)
    }

    override fun set(key: String, value: String) = transaction {
        SystemConfigurationTable.upsert(SystemConfigurationTable.key) {
            it[SystemConfigurationTable.key] = key
            it[SystemConfigurationTable.value] = value
            it[updatedAt] = Instant.now()
        }
        Unit
    }
}
