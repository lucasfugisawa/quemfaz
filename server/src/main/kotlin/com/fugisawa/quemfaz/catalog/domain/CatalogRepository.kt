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
