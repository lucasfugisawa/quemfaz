# Service Catalog Evolution — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the hardcoded service catalog to a database-backed architecture with signal capture for unmatched services, provisional service creation with governance, and safety guardrails.

**Architecture:** Database-backed catalog (PostgreSQL) with in-memory caching on the server. New `catalog` feature module on the server following existing Clean Architecture patterns (domain → application → infrastructure → routing). Shared module retains DTOs only. Client fetches catalog via API with ETag caching. LLM interpreters extended to report unmatched services with safety classification.

**Tech Stack:** Kotlin, Ktor, Exposed ORM, Flyway, PostgreSQL, Koin DI, kotlinx.serialization, ai.koog (LLM), Compose Multiplatform

**Spec:** `docs/superpowers/specs/2026-03-15-service-catalog-design.md`

---

## Chunk 1: Database Schema & Catalog Domain

### Task 1: Flyway Migration — Create catalog tables

**Files:**
- Create: `server/src/main/resources/db/migration/V11__service_catalog.sql`

This migration creates the `service_categories`, `canonical_services`, `unmatched_service_signals`, and `system_configuration` tables. It seeds the 7 categories and 22 services (excluding `OTHER` category and `other-general` service). It also inserts the default `catalog.auto-provisioning.enabled = false` configuration.

- [ ] **Step 1: Write the migration SQL**

```sql
-- Service Categories
CREATE TABLE service_categories (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Canonical Services
CREATE TABLE canonical_services (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    description TEXT NOT NULL,
    category_id TEXT NOT NULL REFERENCES service_categories(id),
    aliases JSONB NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'active',
    created_by TEXT NOT NULL DEFAULT 'migration',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    merged_into_service_id TEXT REFERENCES canonical_services(id),
    review_status_reason TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by TEXT,
    CONSTRAINT valid_status CHECK (status IN ('active', 'pending_review', 'rejected', 'merged'))
);

CREATE INDEX idx_canonical_services_category ON canonical_services(category_id);
CREATE INDEX idx_canonical_services_status ON canonical_services(status);

-- Unmatched Service Signals
CREATE TABLE unmatched_service_signals (
    id TEXT PRIMARY KEY,
    raw_description TEXT NOT NULL,
    source TEXT NOT NULL,
    user_id TEXT REFERENCES users(id),
    best_match_service_id TEXT,
    best_match_confidence TEXT,
    provisional_service_id TEXT REFERENCES canonical_services(id),
    city_name TEXT,
    safety_classification TEXT,
    safety_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_source CHECK (source IN ('onboarding', 'search')),
    CONSTRAINT valid_confidence CHECK (best_match_confidence IN ('high', 'low', 'none') OR best_match_confidence IS NULL),
    CONSTRAINT valid_safety CHECK (safety_classification IN ('safe', 'unsafe', 'uncertain') OR safety_classification IS NULL)
);

CREATE INDEX idx_unmatched_signals_provisional ON unmatched_service_signals(provisional_service_id);
CREATE INDEX idx_unmatched_signals_created ON unmatched_service_signals(created_at);

-- System Configuration
CREATE TABLE system_configuration (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed categories (7 — excluding OTHER)
INSERT INTO service_categories (id, display_name, sort_order) VALUES
    ('CLEANING', 'Limpeza', 1),
    ('REPAIRS', 'Reparos', 2),
    ('PAINTING', 'Pintura', 3),
    ('GARDEN', 'Jardim', 4),
    ('EVENTS', 'Eventos', 5),
    ('BEAUTY', 'Beleza', 6),
    ('MOVING_AND_ASSEMBLY', 'Mudanças e Montagem', 7);

-- Seed services (22 — excluding other-general)
INSERT INTO canonical_services (id, display_name, description, category_id, aliases, status, created_by) VALUES
    ('clean-house', 'Limpeza Residencial', 'Limpeza de casas e apartamentos', 'CLEANING', '["diarista", "faxina", "limpeza de casa"]', 'active', 'migration'),
    ('clean-post-construction', 'Limpeza Pós-Obra', 'Limpeza pesada após construções ou reformas', 'CLEANING', '["pós obra", "limpeza pesada"]', 'active', 'migration'),
    ('clean-sofa', 'Limpeza de Sofá', 'Higienização de estofados, sofás e poltronas', 'CLEANING', '["lavagem de sofá", "impermeabilização", "estofados"]', 'active', 'migration'),
    ('clean-land', 'Limpeza de Terreno', 'Retirada de entulho e mato de terrenos', 'CLEANING', '["roçagem de terreno", "limpar lote"]', 'active', 'migration'),
    ('repair-electrician', 'Eletricista Residencial', 'Instalações e reparos elétricos', 'REPAIRS', '["eletricista", "troca de fiação", "curto circuito"]', 'active', 'migration'),
    ('repair-shower', 'Troca de Chuveiro', 'Instalação ou troca de chuveiros elétricos', 'REPAIRS', '["instalar chuveiro", "chuveiro queimado"]', 'active', 'migration'),
    ('repair-plumber', 'Encanador / Hidráulica', 'Reparos de vazamentos e tubulações', 'REPAIRS', '["encanador", "vazamento", "desentupimento"]', 'active', 'migration'),
    ('repair-handyman', 'Marido de Aluguel', 'Pequenos reparos diversos em residências', 'REPAIRS', '["pequenos consertos", "pendurar quadros", "reparos gerais"]', 'active', 'migration'),
    ('repair-furniture-assembly', 'Montagem de Móveis', 'Montagem e desmontagem de móveis em geral', 'REPAIRS', '["montador de móveis", "armário", "guarda-roupa"]', 'active', 'migration'),
    ('paint-residential', 'Pintura Residencial', 'Pintura de paredes, tetos e fachadas residenciais', 'PAINTING', '["pintor", "pintura de casa", "envernizamento"]', 'active', 'migration'),
    ('paint-commercial', 'Pintura Comercial', 'Serviços de pintura para lojas e escritórios', 'PAINTING', '["pintura de loja", "pintura de galpão"]', 'active', 'migration'),
    ('garden-maintenance', 'Jardinagem', 'Manutenção de jardins e vasos', 'GARDEN', '["jardineiro", "cuidar de plantas"]', 'active', 'migration'),
    ('garden-pruning', 'Poda de Árvores', 'Corte e manutenção de árvores e arbustos', 'GARDEN', '["podar árvore", "corte de galhos"]', 'active', 'migration'),
    ('garden-mowing', 'Roçagem / Corte de Grama', 'Corte de grama e manutenção de gramados', 'GARDEN', '["cortar grama", "roçador"]', 'active', 'migration'),
    ('event-waiter', 'Garçom para Eventos', 'Serviço de garçom para festas e recepções', 'EVENTS', '["garçom", "atendimento de mesas"]', 'active', 'migration'),
    ('event-bartender', 'Bartender / Drinks', 'Preparo de drinks e coquetéis para eventos', 'EVENTS', '["barman", "coquetelaria"]', 'active', 'migration'),
    ('event-kitchen-assistant', 'Ajudante de Cozinha', 'Auxílio no preparo de alimentos em eventos', 'EVENTS', '["auxiliar de cozinha", "copa"]', 'active', 'migration'),
    ('beauty-manicure', 'Manicure e Pedicure', 'Cuidados com as unhas das mãos e pés', 'BEAUTY', '["unhas", "esmaltação", "manicure"]', 'active', 'migration'),
    ('beauty-hairdresser', 'Cabeleireiro(a)', 'Corte, coloração e tratamentos capilares', 'BEAUTY', '["corte de cabelo", "escova", "luzes"]', 'active', 'migration'),
    ('beauty-makeup', 'Maquiador(a)', 'Maquiagem profissional para eventos e fotos', 'BEAUTY', '["maquiagem", "make"]', 'active', 'migration'),
    ('logistic-moving-help', 'Ajudante de Mudança', 'Auxílio no carregamento e transporte de mudanças', 'MOVING_AND_ASSEMBLY', '["carreto", "ajuda com mudança"]', 'active', 'migration'),
    ('logistic-freight', 'Pequenos Fretes / Carretos', 'Transporte de cargas leves e móveis', 'MOVING_AND_ASSEMBLY', '["carreto", "fretinho", "entrega"]', 'active', 'migration');

-- Default configuration: auto-provisioning disabled
INSERT INTO system_configuration (key, value) VALUES
    ('catalog.auto-provisioning.enabled', 'false');
```

- [ ] **Step 2: Run migration to verify it succeeds**

Run: `./gradlew :server:test --tests "*.integration.*" -x compileCommonMainKotlinMetadata 2>&1 | head -20`

The migration will run as part of Testcontainers setup. Any SQL errors will surface here.

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/V11__service_catalog.sql
git commit -m "feat: add V11 migration for service catalog tables and seed data"
```

---

### Task 2: Catalog domain models (server)

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/domain/Models.kt`

Domain models for the catalog feature module. Follows the same pattern as `profile/domain/Models.kt` and `search/domain/Models.kt`.

- [ ] **Step 1: Write domain models**

```kotlin
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
            entries.first { it.name.equals(value, ignoreCase = true) }
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
```

- [ ] **Step 2: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/domain/Models.kt
git commit -m "feat: add catalog domain models"
```

---

### Task 3: Catalog repository interface and Exposed implementation

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/domain/CatalogRepository.kt`
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/infrastructure/persistence/ExposedCatalogRepository.kt`

Follows the pattern of `ExposedProfessionalProfileRepository`: interface in `domain/`, Exposed tables + implementation in `infrastructure/persistence/`.

- [ ] **Step 1: Write the repository interface**

```kotlin
package com.fugisawa.quemfaz.catalog.domain

interface CatalogRepository {
    fun findAllCategories(): List<ServiceCategory>
    fun findServicesByStatus(vararg statuses: CatalogServiceStatus): List<CatalogServiceRecord>
    fun findServiceById(id: String): CatalogServiceRecord?
    fun createService(service: CatalogServiceRecord): CatalogServiceRecord
    fun updateServiceStatus(
        serviceId: String,
        newStatus: CatalogServiceStatus,
        reason: String? = null,
        reviewedBy: String? = null,
        mergedIntoServiceId: String? = null,
    )
    fun migrateProfileServices(fromServiceId: String, toServiceId: String)
    fun removeServiceFromProfiles(serviceId: String)
}

interface SignalRepository {
    fun create(signal: UnmatchedServiceSignal)
    fun findByProvisionalServiceId(provisionalServiceId: String): List<UnmatchedServiceSignal>
}

interface SystemConfigRepository {
    fun get(key: String): String?
    fun set(key: String, value: String)
}
```

- [ ] **Step 2: Write Exposed table definitions and repository implementation**

```kotlin
package com.fugisawa.quemfaz.catalog.infrastructure.persistence

import com.fugisawa.quemfaz.catalog.domain.*
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfileServicesTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
    val source = text("source")
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
    source = this[UnmatchedServiceSignalsTable.source],
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
            it[reviewStatusReason] = reason
            it[CanonicalServicesTable.reviewedBy] = reviewedBy
            it[reviewedAt] = now
            it[updatedAt] = now
            if (mergedIntoServiceId != null) {
                it[CanonicalServicesTable.mergedIntoServiceId] = mergedIntoServiceId
            }
        }
        Unit
    }

    override fun migrateProfileServices(fromServiceId: String, toServiceId: String) = transaction {
        // Delete any existing rows for the target service to avoid PK conflicts
        ProfessionalProfileServicesTable.deleteWhere {
            (ProfessionalProfileServicesTable.serviceId eq toServiceId) and
                (ProfessionalProfileServicesTable.professionalProfileId inSubQuery
                    ProfessionalProfileServicesTable
                        .select(ProfessionalProfileServicesTable.professionalProfileId)
                        .where { ProfessionalProfileServicesTable.serviceId eq fromServiceId })
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
            it[source] = signal.source
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
        val exists = SystemConfigurationTable
            .selectAll()
            .where { SystemConfigurationTable.key eq key }
            .count() > 0
        if (exists) {
            SystemConfigurationTable.update({ SystemConfigurationTable.key eq key }) {
                it[SystemConfigurationTable.value] = value
                it[updatedAt] = Instant.now()
            }
        } else {
            SystemConfigurationTable.insert {
                it[SystemConfigurationTable.key] = key
                it[SystemConfigurationTable.value] = value
            }
        }
        Unit
    }
}
```

**Note:** The `ProfessionalProfileServicesTable` is already defined in the existing `ExposedProfessionalProfileRepository.kt`. You will need to verify the exact table/column names match. The table is named `professional_profile_services` with columns `professional_profile_id`, `service_id`, `match_level`.

- [ ] **Step 3: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/domain/CatalogRepository.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/infrastructure/persistence/ExposedCatalogRepository.kt
git commit -m "feat: add catalog repository interface and Exposed implementation"
```

---

### Task 4: CatalogService — in-memory cached catalog access

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/application/CatalogService.kt`

This is the server-side replacement for `CanonicalServices.kt`. It loads from DB, caches in memory, and provides the same lookup/search operations. All existing code that references `CanonicalServices` will eventually point here.

- [ ] **Step 1: Write CatalogService**

```kotlin
package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

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
```

**Note:** `CatalogEntry` is a lightweight cache-friendly view. `CatalogServiceRecord` is the full domain model. `CatalogService` is the application service class. The three names are intentionally distinct to avoid Kotlin naming collisions.

- [ ] **Step 2: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/application/CatalogService.kt
git commit -m "feat: add CatalogService with in-memory caching and search"
```

---

### Task 5: Register catalog module in Koin DI

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`

Register the new catalog repositories, CatalogService, and wire them into existing services that currently depend on `CanonicalServices`.

- [ ] **Step 1: Add catalog DI registrations**

Add imports at the top:
```kotlin
import com.fugisawa.quemfaz.catalog.domain.CatalogRepository
import com.fugisawa.quemfaz.catalog.domain.SignalRepository
import com.fugisawa.quemfaz.catalog.domain.SystemConfigRepository
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.ExposedCatalogRepository
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.ExposedSignalRepository
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.ExposedSystemConfigRepository
import com.fugisawa.quemfaz.catalog.application.CatalogService
```

Add registrations in `infrastructureModule` after the LLM Agent Service block (after line 141):
```kotlin
// Catalog
single<CatalogRepository> { ExposedCatalogRepository() }
single<SignalRepository> { ExposedSignalRepository() }
single<SystemConfigRepository> { ExposedSystemConfigRepository() }
single { CatalogService(get(), get()) }
```

- [ ] **Step 2: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "feat: register catalog module in Koin DI"
```

---

### Task 6: Integration test — catalog repository

**Files:**
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/catalog/CatalogRepositoryIntegrationTest.kt`

Verify that the migration seeds data correctly and that repository operations work.

- [ ] **Step 1: Write integration test**

```kotlin
package com.fugisawa.quemfaz.integration.catalog

import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.catalog.domain.UnmatchedServiceSignal
import com.fugisawa.quemfaz.catalog.infrastructure.persistence.*
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import org.jetbrains.exposed.sql.Table
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogRepositoryIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = listOf(
        UnmatchedServiceSignalsTable,
        // Do NOT clean CanonicalServicesTable or ServiceCategoriesTable — they are seeded by migration
    )

    @Test
    fun `migration seeds 7 categories`() = integrationTestApplication {
        val repo = ExposedCatalogRepository()
        val categories = repo.findAllCategories()
        assertEquals(7, categories.size)
        assertTrue(categories.any { it.id == "CLEANING" })
        assertTrue(categories.none { it.id == "OTHER" })
    }

    @Test
    fun `migration seeds 22 active services`() = integrationTestApplication {
        val repo = ExposedCatalogRepository()
        val services = repo.findServicesByStatus(CatalogServiceStatus.ACTIVE)
        assertEquals(22, services.size)
        assertTrue(services.none { it.id == "other-general" })
    }

    @Test
    fun `findServiceById returns seeded service`() = integrationTestApplication {
        val repo = ExposedCatalogRepository()
        val service = repo.findServiceById("clean-house")
        assertNotNull(service)
        assertEquals("Limpeza Residencial", service.displayName)
        assertEquals("CLEANING", service.categoryId)
        assertEquals(CatalogServiceStatus.ACTIVE, service.status)
        assertTrue(service.aliases.contains("diarista"))
    }

    @Test
    fun `findServiceById returns null for nonexistent`() = integrationTestApplication {
        val repo = ExposedCatalogRepository()
        assertNull(repo.findServiceById("nonexistent"))
    }

    @Test
    fun `signal repository creates and retrieves signals`() = integrationTestApplication {
        val repo = ExposedSignalRepository()
        val signal = UnmatchedServiceSignal(
            id = UUID.randomUUID().toString(),
            rawDescription = "instalo câmeras de segurança",
            source = "onboarding",
            userId = null,
            bestMatchServiceId = "repair-electrician",
            bestMatchConfidence = "low",
            provisionalServiceId = null,
            cityName = "São Paulo",
            safetyClassification = "safe",
            safetyReason = null,
            createdAt = Instant.now(),
        )
        repo.create(signal)
        // Retrieve by provisional service ID (null) should return empty
        val signals = repo.findByProvisionalServiceId("some-id")
        assertTrue(signals.isEmpty())
    }

    @Test
    fun `system config repository reads default value`() = integrationTestApplication {
        val repo = ExposedSystemConfigRepository()
        val value = repo.get("catalog.auto-provisioning.enabled")
        assertEquals("false", value)
    }

    @Test
    fun `system config repository writes and reads`() = integrationTestApplication {
        val repo = ExposedSystemConfigRepository()
        repo.set("catalog.auto-provisioning.enabled", "true")
        assertEquals("true", repo.get("catalog.auto-provisioning.enabled"))
        // Reset
        repo.set("catalog.auto-provisioning.enabled", "false")
    }
}
```

- [ ] **Step 2: Run the integration tests**

Run: `./gradlew :server:test --tests "*.integration.catalog.*" 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/catalog/CatalogRepositoryIntegrationTest.kt
git commit -m "test: add catalog repository integration tests"
```

---

## Chunk 2: Shared DTOs & Catalog API Endpoint

### Task 7: Update shared DTOs

**Files:**
- Create: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/catalog/CatalogDtos.kt`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/search/SearchDtos.kt`

Add the new catalog DTOs and extend existing DTOs with `status` and `blockedDescriptions` fields.

- [ ] **Step 1: Create catalog DTOs**

```kotlin
package com.fugisawa.quemfaz.contract.catalog

import kotlinx.serialization.Serializable

@Serializable
data class CatalogResponse(
    val version: String,
    val categories: List<ServiceCategoryDto>,
    val services: List<CatalogServiceDto>,
)

@Serializable
data class ServiceCategoryDto(
    val id: String,
    val displayName: String,
    val sortOrder: Int,
)

@Serializable
data class CatalogServiceDto(
    val id: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
)
```

- [ ] **Step 2: Add `status` to `InterpretedServiceDto`**

In `ProfileDtos.kt`, change `InterpretedServiceDto` to:
```kotlin
@Serializable
data class InterpretedServiceDto(
    val serviceId: String,
    val displayName: String,
    val matchLevel: String,
    val status: String = "active",
)
```

- [ ] **Step 3: Add `blockedDescriptions` to response DTOs**

In `ProfileDtos.kt`, add to `CreateProfessionalProfileDraftResponse`:
```kotlin
val blockedDescriptions: List<String> = emptyList(),
```

In `SearchDtos.kt`, add to `SearchProfessionalsResponse`:
```kotlin
val blockedDescriptions: List<String> = emptyList(),
```

- [ ] **Step 4: Compile shared module**

Run: `./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Compile server (verify backward compat)**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (defaults ensure backward compatibility)

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/catalog/CatalogDtos.kt
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/profile/ProfileDtos.kt
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/search/SearchDtos.kt
git commit -m "feat: add catalog DTOs and extend existing DTOs with status and blockedDescriptions"
```

---

### Task 8: Catalog API endpoint

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/routing/CatalogRoutes.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt`

Public endpoint (no auth required) that returns the active catalog with ETag support.

- [ ] **Step 1: Write CatalogRoutes**

```kotlin
package com.fugisawa.quemfaz.catalog.routing

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.catalog.CatalogServiceDto
import com.fugisawa.quemfaz.contract.catalog.ServiceCategoryDto
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.catalogRoutes() {
    val catalogService by inject<CatalogService>()

    route("/services") {
        get("/catalog") {
            val clientVersion = call.request.headers["If-None-Match"]
            val currentVersion = catalogService.getCatalogVersion()

            if (clientVersion == currentVersion) {
                call.respond(HttpStatusCode.NotModified)
                return@get
            }

            val categories = catalogService.getCategories().map {
                ServiceCategoryDto(it.id, it.displayName, it.sortOrder)
            }
            val services = catalogService.getActiveServices().map {
                CatalogServiceDto(it.id, it.displayName, it.description, it.categoryId, it.aliases)
            }

            call.response.header(HttpHeaders.ETag, currentVersion)
            call.respond(CatalogResponse(currentVersion, categories, services))
        }
    }
}
```

- [ ] **Step 2: Register route in Application.kt**

Add import:
```kotlin
import com.fugisawa.quemfaz.catalog.routing.catalogRoutes
```

Add `catalogRoutes()` inside the `routing { }` block (after line 102):
```kotlin
catalogRoutes()
```

- [ ] **Step 3: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/routing/CatalogRoutes.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt
git commit -m "feat: add GET /services/catalog endpoint with ETag caching"
```

---

### Task 9: Integration test — catalog endpoint

**Files:**
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/catalog/CatalogEndpointIntegrationTest.kt`

- [ ] **Step 1: Write integration test**

```kotlin
package com.fugisawa.quemfaz.integration.catalog

import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.integration.BaseIntegrationTest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogEndpointIntegrationTest : BaseIntegrationTest() {
    override val tablesToClean: List<Table> = emptyList()

    @Test
    fun `GET catalog returns active services and categories`() = integrationTestApplication {
        val client = createTestClient()
        val response = client.get("/services/catalog")
        assertEquals(HttpStatusCode.OK, response.status)

        val catalog = response.body<CatalogResponse>()
        assertEquals(7, catalog.categories.size)
        assertEquals(22, catalog.services.size)
        assertTrue(catalog.version.isNotBlank())
        assertTrue(catalog.services.none { it.id == "other-general" })
    }

    @Test
    fun `GET catalog returns 304 when ETag matches`() = integrationTestApplication {
        val client = createTestClient()
        val firstResponse = client.get("/services/catalog")
        val etag = firstResponse.headers[HttpHeaders.ETag]!!

        val secondResponse = client.get("/services/catalog") {
            header("If-None-Match", etag)
        }
        assertEquals(HttpStatusCode.NotModified, secondResponse.status)
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :server:test --tests "*.integration.catalog.CatalogEndpointIntegrationTest" 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/catalog/CatalogEndpointIntegrationTest.kt
git commit -m "test: add catalog endpoint integration tests"
```

---

## Chunk 3: LLM Interpreter Changes & Signal Capture

### Task 10: Update LLM models for unmatched descriptions

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmModels.kt`

Extend the LLM response structures with `unmatchedDescriptions` and safety classification.

- [ ] **Step 1: Update LlmModels.kt**

Replace the file contents with:
```kotlin
package com.fugisawa.quemfaz.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnmatchedDescription(
    val rawDescription: String,
    val safetyClassification: String = "safe",
    val safetyReason: String? = null,
)

@Serializable
@SerialName("OnboardingInterpretation")
data class OnboardingInterpretation(
    val serviceIds: List<String>,
    val needsClarification: Boolean,
    val clarificationQuestions: List<String> = emptyList(),
    val unmatchedDescriptions: List<UnmatchedDescription> = emptyList(),
)

@Serializable
@SerialName("SearchInterpretation")
data class SearchInterpretation(
    val serviceIds: List<String> = emptyList(),
    val unmatchedDescriptions: List<UnmatchedDescription> = emptyList(),
)
```

**Note:** `SearchInterpretation.serviceId` (singular) is changed to `serviceIds` (list) for consistency with `OnboardingInterpretation`. All consumers of `SearchInterpretation` must be updated in the same commit — see Task 11.

- [ ] **Step 2: Compile (will fail — consumers not yet updated)**

This is expected to fail. Proceed to Task 11 to update consumers.

---

### Task 11: Rewrite LlmSearchQueryInterpreter

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt`

Rewrite to use `CatalogService` instead of `CanonicalServices`, use updated `SearchInterpretation` model, capture signals, and handle provisional services.

- [ ] **Step 1: Rewrite LlmSearchQueryInterpreter**

```kotlin
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
```

- [ ] **Step 2: Update `InterpretedSearchQuery` to include `blockedDescriptions`**

In `server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/Models.kt`, add:
```kotlin
data class InterpretedSearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val serviceIds: List<String>,
    val cityName: String?,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
    val blockedDescriptions: List<String> = emptyList(),
)
```

- [ ] **Step 3: Update `SearchProfessionalsService` to use `CatalogService` and pass through `blockedDescriptions`**

In `server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt`:

Replace the `CanonicalServices` import with:
```kotlin
import com.fugisawa.quemfaz.catalog.application.CatalogService
```

Add `catalogService: CatalogService` to the constructor.

Replace all `CanonicalServices.findById(CanonicalServiceId(serviceId))` calls with `catalogService.findById(serviceId)`.

Add `blockedDescriptions = interpreted.blockedDescriptions` to the `SearchProfessionalsResponse` constructor.

- [ ] **Step 4: Update `LlmSearchQueryInterpreter` constructor in KoinModules.kt**

Change line 159:
```kotlin
single<SearchQueryInterpreter> { LlmSearchQueryInterpreter(get(), get(), get()) }
```

Update `SearchProfessionalsService` registration (line 165) to include `CatalogService`:
```kotlin
single { SearchProfessionalsService(get(), get(), get(), get(), get(), get()) }
```

- [ ] **Step 5: Update `MockSearchQueryInterpreter`**

In `server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/MockSearchQueryInterpreter.kt`, update to use `CatalogService` instead of `CanonicalServices`. The mock should accept `CatalogService` in its constructor and use `catalogService.getActiveServices()` instead of `CanonicalServices.all`.

- [ ] **Step 6: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/llm/LlmModels.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/MockSearchQueryInterpreter.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/domain/Models.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/application/SearchProfessionalsService.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "feat: update search interpreter to use CatalogService, capture signals, report unmatched"
```

---

### Task 12: Rewrite LlmProfessionalInputInterpreter

**Files:**
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/MockProfessionalInputInterpreter.kt`

Same pattern as Task 11 but for the onboarding flow. Uses `CatalogService`, captures signals, handles `unmatchedDescriptions`, and reports `blockedDescriptions`.

- [ ] **Step 1: Rewrite LlmProfessionalInputInterpreter**

Replace `CanonicalServices` usage with `CatalogService`. Add `SignalRepository` dependency. Process `unmatchedDescriptions` from the LLM response. Populate `blockedDescriptions` in the response DTO.

Key changes:
- Constructor: `(llmAgentService: LlmAgentService, catalogService: CatalogService, signalRepository: SignalRepository)`
- `mapToResponse()`: iterate `interpretation.unmatchedDescriptions`, capture signals, collect blocked descriptions, add to response
- `fallbackResponse()`: capture signal when no matches found
- `SYSTEM_PROMPT`: dynamically built from `catalogService.getActiveServices()`, with updated instructions matching the spec (no `other-general` reference, report unmatched, classify safety)
- `companion object` with static prompt removed — prompt is now built dynamically via `buildSystemPrompt()`

The full rewrite follows the same structure as Task 11's `LlmSearchQueryInterpreter` — signal capture, safety classification, blocked descriptions.

- [ ] **Step 2: Update MockProfessionalInputInterpreter**

Update to accept `CatalogService` in constructor and use `catalogService.getActiveServices()` and `catalogService.search()` instead of `CanonicalServices`.

- [ ] **Step 3: Update KoinModules.kt**

Update the interpreter registration:
```kotlin
single<ProfessionalInputInterpreter> { LlmProfessionalInputInterpreter(get(), get(), get()) }
```

- [ ] **Step 4: Update `ConfirmProfessionalProfileService`**

In `server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt`:
- Replace `CanonicalServices` import with `CatalogService`
- Add `catalogService` to constructor
- Replace `CanonicalServices.findById(...)` calls with `catalogService.findById(...)`
- Update the service's Koin registration in `KoinModules.kt`

- [ ] **Step 5: Update remaining `CanonicalServices` usages on server**

Search for any remaining `import com.fugisawa.quemfaz.domain.service.CanonicalServices` in the server module. Each occurrence must be replaced with `CatalogService` injection. Key files to migrate:
- `FavoriteServices.kt` — uses `CanonicalServices.findById()` to resolve display names when listing favorites. Add `CatalogService` to the constructor, replace the import, and update the Koin registration.
- `GetMyProfessionalProfileService` / `GetPublicProfessionalProfileService` — if they resolve display names
- Any other service or route that references `CanonicalServices`

Run `grep -r "import com.fugisawa.quemfaz.domain.service.CanonicalServices" server/src/main/` to verify zero remaining references before proceeding.

- [ ] **Step 6: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Run all server tests**

Run: `./gradlew :server:test 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/MockProfessionalInputInterpreter.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/application/ProfileServices.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "feat: update onboarding interpreter to use CatalogService, capture signals, report unmatched"
```

---

### Task 12.5: Provisional service creation logic

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/application/ProvisionalServiceCreator.kt`

This implements the core auto-provisioning flow described in the spec's Section 3: when an unmatched service is detected and the auto-provisioning flag is enabled, generate a candidate service definition via LLM, deduplicate, and create a `pending_review` entry.

- [ ] **Step 1: Write ProvisionalServiceCreator**

```kotlin
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

    /**
     * Attempts to create a provisional service for an unmatched description.
     * Returns the provisional service ID if created or matched, null if provisioning is disabled or failed.
     */
    fun tryProvision(
        rawDescription: String,
        source: String,
        userId: String?,
        cityName: String?,
        safetyClassification: String?,
        safetyReason: String?,
    ): String? {
        // Check if auto-provisioning is enabled
        if (!catalogService.isAutoProvisioningEnabled()) return null

        // Only provision safe or uncertain descriptions
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
                // Active or other status — don't create duplicate
                return null
            }

            // Deduplication: check display name (case-insensitive exact match)
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
```

- [ ] **Step 2: Integrate into interpreters**

In both `LlmSearchQueryInterpreter` and `LlmProfessionalInputInterpreter`, add `ProvisionalServiceCreator` as a constructor dependency. In the `captureSignal`-equivalent code path, after capturing the signal, call `provisionalServiceCreator.tryProvision(...)`. If it returns a service ID, add it to the response's service list and update the signal record's `provisionalServiceId`.

Update the signal capture logic:
```kotlin
val provisionalId = provisionalServiceCreator.tryProvision(
    rawDescription, source, userId, cityName, safetyClassification, safetyReason
)
// Create signal with provisionalServiceId
signalRepository.create(signal.copy(provisionalServiceId = provisionalId))
```

- [ ] **Step 3: Register in Koin DI**

In `KoinModules.kt`:
```kotlin
single { ProvisionalServiceCreator(get(), get(), get(), get()) }
```

Update interpreter registrations to include the new dependency:
```kotlin
single<SearchQueryInterpreter> { LlmSearchQueryInterpreter(get(), get(), get(), get()) }
single<ProfessionalInputInterpreter> { LlmProfessionalInputInterpreter(get(), get(), get(), get()) }
```

- [ ] **Step 4: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/application/ProvisionalServiceCreator.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/search/interpretation/LlmSearchQueryInterpreter.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/profile/interpretation/LlmProfessionalInputInterpreter.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git commit -m "feat: add provisional service creation with LLM candidate generation and deduplication"
```

---

## Chunk 4: Admin API & Governance

### Task 13: Admin review endpoints

**Files:**
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/application/AdminCatalogService.kt`
- Create: `server/src/main/kotlin/com/fugisawa/quemfaz/catalog/routing/AdminCatalogRoutes.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt`
- Modify: `server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt`

- [ ] **Step 1: Write AdminCatalogService**

```kotlin
package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.*
import org.slf4j.LoggerFactory

class AdminCatalogService(
    private val catalogRepository: CatalogRepository,
    private val signalRepository: SignalRepository,
    private val catalogService: CatalogService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun listPendingServices(): List<PendingServiceView> {
        val pending = catalogRepository.findServicesByStatus(CatalogServiceStatus.PENDING_REVIEW)
        return pending.map { service ->
            val signals = signalRepository.findByProvisionalServiceId(service.id)
            PendingServiceView(
                service = service,
                signalCount = signals.size,
                sources = signals.map { it.source }.distinct(),
                cities = signals.mapNotNull { it.cityName }.distinct(),
            )
        }
    }

    fun approveService(serviceId: String, reviewedBy: String) {
        val service = catalogRepository.findServiceById(serviceId)
            ?: throw IllegalArgumentException("Service not found: $serviceId")
        require(service.status == CatalogServiceStatus.PENDING_REVIEW) {
            "Only pending_review services can be approved. Current status: ${service.status}"
        }
        catalogRepository.updateServiceStatus(serviceId, CatalogServiceStatus.ACTIVE, reviewedBy = reviewedBy)
        catalogService.refreshCache()
        logger.info("Service approved: {} by {}", serviceId, reviewedBy)
    }

    fun rejectService(serviceId: String, reason: String, reviewedBy: String) {
        val service = catalogRepository.findServiceById(serviceId)
            ?: throw IllegalArgumentException("Service not found: $serviceId")
        require(service.status == CatalogServiceStatus.PENDING_REVIEW) {
            "Only pending_review services can be rejected. Current status: ${service.status}"
        }
        catalogRepository.removeServiceFromProfiles(serviceId)
        catalogRepository.updateServiceStatus(serviceId, CatalogServiceStatus.REJECTED, reason, reviewedBy)
        catalogService.refreshCache()
        logger.info("Service rejected: {} by {} — reason: {}", serviceId, reviewedBy, reason)
    }

    fun mergeService(serviceId: String, mergeIntoServiceId: String, reason: String, reviewedBy: String) {
        val service = catalogRepository.findServiceById(serviceId)
            ?: throw IllegalArgumentException("Service not found: $serviceId")
        require(service.status == CatalogServiceStatus.PENDING_REVIEW) {
            "Only pending_review services can be merged. Current status: ${service.status}"
        }
        val target = catalogRepository.findServiceById(mergeIntoServiceId)
            ?: throw IllegalArgumentException("Target service not found: $mergeIntoServiceId")
        require(target.status == CatalogServiceStatus.ACTIVE) {
            "Can only merge into active services. Target status: ${target.status}"
        }
        catalogRepository.migrateProfileServices(serviceId, mergeIntoServiceId)
        catalogRepository.updateServiceStatus(serviceId, CatalogServiceStatus.MERGED, reason, reviewedBy, mergeIntoServiceId)
        catalogService.refreshCache()
        logger.info("Service merged: {} → {} by {} — reason: {}", serviceId, mergeIntoServiceId, reviewedBy, reason)
    }
}

data class PendingServiceView(
    val service: CatalogServiceRecord,
    val signalCount: Int,
    val sources: List<String>,
    val cities: List<String>,
)
```

- [ ] **Step 2: Write admin DTOs**

Add to `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/catalog/CatalogDtos.kt`:

```kotlin
@Serializable
data class PendingServiceResponse(
    val id: String,
    val displayName: String,
    val description: String,
    val categoryId: String,
    val aliases: List<String>,
    val signalCount: Int,
    val sources: List<String>,
    val cities: List<String>,
    val createdAt: String,
)

@Serializable
data class ReviewServiceRequest(
    val reason: String = "",
    val mergeIntoServiceId: String? = null,
)
```

- [ ] **Step 3: Write AdminCatalogRoutes**

```kotlin
package com.fugisawa.quemfaz.catalog.routing

import com.fugisawa.quemfaz.catalog.application.AdminCatalogService
import com.fugisawa.quemfaz.contract.catalog.PendingServiceResponse
import com.fugisawa.quemfaz.contract.catalog.ReviewServiceRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminCatalogRoutes() {
    val adminCatalogService by inject<AdminCatalogService>()

    authenticate("auth-jwt") {
        route("/admin/catalog") {
            get("/pending") {
                val pending = adminCatalogService.listPendingServices()
                call.respond(pending.map { view ->
                    PendingServiceResponse(
                        id = view.service.id,
                        displayName = view.service.displayName,
                        description = view.service.description,
                        categoryId = view.service.categoryId,
                        aliases = view.service.aliases,
                        signalCount = view.signalCount,
                        sources = view.sources,
                        cities = view.cities,
                        createdAt = view.service.createdAt.toString(),
                    )
                })
            }

            post("/{serviceId}/approve") {
                val serviceId = call.parameters["serviceId"]!!
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                try {
                    adminCatalogService.approveService(serviceId, userId)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "approved"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            post("/{serviceId}/reject") {
                val serviceId = call.parameters["serviceId"]!!
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<ReviewServiceRequest>()
                try {
                    adminCatalogService.rejectService(serviceId, request.reason, userId)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "rejected"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            post("/{serviceId}/merge") {
                val serviceId = call.parameters["serviceId"]!!
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<ReviewServiceRequest>()
                val mergeIntoId = request.mergeIntoServiceId
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "mergeIntoServiceId required"))
                try {
                    adminCatalogService.mergeService(serviceId, mergeIntoId, request.reason, userId)
                    call.respond(HttpStatusCode.OK, mapOf("status" to "merged"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
        }
    }
}
```

**Important:** The admin endpoints use JWT auth for now (any authenticated user can access). **Before deployment, a basic admin role check must be added** — at minimum a hardcoded list of admin user IDs checked in the route handler. This is a security requirement, not a nice-to-have. A proper role-based access system can come later, but unauthenticated access to approve/reject/merge must be prevented.

- [ ] **Step 4: Register in DI and Application**

In `KoinModules.kt`:
```kotlin
single { AdminCatalogService(get(), get(), get()) }
```

In `Application.kt`:
```kotlin
import com.fugisawa.quemfaz.catalog.routing.adminCatalogRoutes
// Inside routing { }:
adminCatalogRoutes()
```

- [ ] **Step 5: Compile to verify**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/application/AdminCatalogService.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/catalog/routing/AdminCatalogRoutes.kt
git add shared/src/commonMain/kotlin/com/fugisawa/quemfaz/contract/catalog/CatalogDtos.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/config/KoinModules.kt
git add server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt
git commit -m "feat: add admin catalog review endpoints (approve/reject/merge)"
```

---

### Task 14: Integration test — admin review workflow

**Files:**
- Create: `server/src/test/kotlin/com/fugisawa/quemfaz/integration/catalog/AdminCatalogIntegrationTest.kt`

- [ ] **Step 1: Write integration test**

Test the full approve/reject/merge lifecycle. This test should:
1. Insert a `pending_review` service directly in the DB
2. Verify it appears in `GET /admin/catalog/pending`
3. Test approve → service becomes active, appears in catalog endpoint
4. Insert another pending service, test reject → service no longer in catalog
5. Insert another pending service, test merge → professionals migrated

- [ ] **Step 2: Run tests**

Run: `./gradlew :server:test --tests "*.integration.catalog.*" 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add server/src/test/kotlin/com/fugisawa/quemfaz/integration/catalog/AdminCatalogIntegrationTest.kt
git commit -m "test: add admin catalog review workflow integration tests"
```

---

## Chunk 5: Retire CanonicalServices & Client Migration

### Task 15: Remove CanonicalServices.kt and ServiceCategory enum from shared

**Files:**
- Delete: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/domain/service/CanonicalServices.kt`
- Modify: `shared/src/commonMain/kotlin/com/fugisawa/quemfaz/domain/service/ServiceModels.kt`
- Delete or update: `shared/src/commonTest/kotlin/com/fugisawa/quemfaz/domain/service/CanonicalServicesTest.kt`

**Important:** This task should only be done AFTER all server-side code has been migrated to use `CatalogService` (Tasks 11-12). Verify by searching for `import com.fugisawa.quemfaz.domain.service.CanonicalServices` across the entire codebase — there should be zero server-side references remaining.

- [ ] **Step 1: Verify no server-side references remain**

Run: `grep -r "import com.fugisawa.quemfaz.domain.service.CanonicalServices" server/`
Expected: No matches

Run: `grep -r "import com.fugisawa.quemfaz.domain.service.ServiceCategory" server/`
Expected: No matches (enum usage migrated to string-based category IDs from DB)

- [ ] **Step 2: Remove `ServiceCategory` enum and `CanonicalService` data class from `ServiceModels.kt`**

Keep only `ServiceMatchLevel` enum, `CanonicalServiceId` (from IDs.kt — already separate), and `ProfessionalService` data class. Remove `ServiceCategory` enum and `CanonicalService` data class.

- [ ] **Step 3: Delete `CanonicalServices.kt`**

- [ ] **Step 4: Delete or rewrite `CanonicalServicesTest.kt`**

The test validated the hardcoded catalog. It's no longer needed — the integration tests (Task 6) validate the DB-seeded catalog. Delete the file.

- [ ] **Step 5: Update client-side code**

In `composeApp/`:
- `ServiceCategoryPicker.kt`: Replace `CanonicalServices.findByCategory(category)` and `ServiceCategory.entries` with data fetched from the catalog API. The component should receive catalog data as a parameter (injected from ViewModel or composable parent).
- `OnboardingViewModel.kt`: Replace `CanonicalServices.findById(CanonicalServiceId(serviceId))` with lookup against locally cached catalog data.
- `HomeViewModel.kt`: Same as OnboardingViewModel — use cached catalog.
- `OnboardingScreens.kt` and `SearchScreens.kt`: Replace `ServiceCategory` enum references with category data from cached catalog.

- [ ] **Step 6: Create a `CatalogApiClient` in composeApp**

Create `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/network/CatalogApiClient.kt`:

```kotlin
package com.fugisawa.quemfaz.network

import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CatalogApiClient(private val client: HttpClient) {
    private var cachedVersion: String? = null
    private var cachedResponse: CatalogResponse? = null

    suspend fun getCatalog(): CatalogResponse {
        val response = client.get("/services/catalog") {
            cachedVersion?.let { header("If-None-Match", it) }
        }

        if (response.status == HttpStatusCode.NotModified && cachedResponse != null) {
            return cachedResponse!!
        }

        val catalog = response.body<CatalogResponse>()
        cachedVersion = catalog.version
        cachedResponse = catalog
        return catalog
    }
}
```

Register in `composeApp/src/commonMain/kotlin/com/fugisawa/quemfaz/di/Koin.kt`:
```kotlin
single { CatalogApiClient(get<ApiClient>().client) }
```

Then update `FeatureApiClients` (or equivalent) to expose `CatalogApiClient`, or inject it directly into ViewModels that need it.

For each ViewModel that previously used `CanonicalServices.findById()`:
- Add a `CatalogApiClient` or cached catalog reference to the constructor
- On ViewModel init (or lazily), fetch catalog via `catalogApiClient.getCatalog()`
- Replace `CanonicalServices.findById(CanonicalServiceId(id))` with a lookup against the fetched `CatalogResponse.services` list

For `ServiceCategoryPicker`:
- Accept `CatalogResponse` (or `categories: List<ServiceCategoryDto>` + `services: List<CatalogServiceDto>`) as parameters instead of importing `CanonicalServices` and `ServiceCategory`
- Group services by `categoryId` using the DTO data
- The parent composable is responsible for fetching and passing the catalog data

- [ ] **Step 7: Compile full project**

Run: `./gradlew compileCommonMainKotlinMetadata :server:compileKotlin :composeApp:compileKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: retire CanonicalServices and ServiceCategory enum, migrate client to catalog API"
```

---

### Task 16: Final verification — run all tests

**Files:** No new files.

- [ ] **Step 1: Run all server tests**

Run: `./gradlew :server:test 2>&1 | tail -20`
Expected: All tests PASS

- [ ] **Step 2: Run shared module tests**

Run: `./gradlew :shared:allTests 2>&1 | tail -10`
Expected: All tests PASS (CanonicalServicesTest deleted, no remaining failures)

- [ ] **Step 3: Build full project**

Run: `./gradlew build 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit any remaining fixes**

If any test fixes were needed, commit them:
```bash
git add -A
git commit -m "fix: resolve remaining test/build issues after catalog migration"
```
