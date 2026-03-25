package com.fugisawa.quemfaz.catalog.application

import com.fugisawa.quemfaz.catalog.domain.CatalogRepository
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceRecord
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.catalog.domain.SignalRepository
import org.slf4j.LoggerFactory

class AdminCatalogService(
    private val catalogRepository: CatalogRepository,
    private val signalRepository: SignalRepository,
    private val catalogService: CatalogService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun listPendingServices(
        limit: Int = 50,
        offset: Int = 0,
    ): List<PendingServiceView> {
        val pending =
            catalogRepository
                .findServicesByStatus(CatalogServiceStatus.PENDING_REVIEW)
                .drop(offset)
                .take(limit)
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

    fun approveService(
        serviceId: String,
        reviewedBy: String,
    ) {
        val service =
            catalogRepository.findServiceById(serviceId)
                ?: throw IllegalArgumentException("Service not found: $serviceId")
        require(service.status == CatalogServiceStatus.PENDING_REVIEW) {
            "Only pending_review services can be approved. Current status: ${service.status}"
        }
        catalogRepository.updateServiceStatus(serviceId, CatalogServiceStatus.ACTIVE, reviewedBy = reviewedBy)
        catalogService.incrementVersion()
        catalogService.refreshCache()
        logger.info("Service approved: {} by {}", serviceId, reviewedBy)
    }

    fun rejectService(
        serviceId: String,
        reason: String,
        reviewedBy: String,
    ) {
        val service =
            catalogRepository.findServiceById(serviceId)
                ?: throw IllegalArgumentException("Service not found: $serviceId")
        require(service.status == CatalogServiceStatus.PENDING_REVIEW) {
            "Only pending_review services can be rejected. Current status: ${service.status}"
        }
        catalogRepository.removeServiceFromProfiles(serviceId)
        catalogRepository.updateServiceStatus(serviceId, CatalogServiceStatus.REJECTED, reason, reviewedBy)
        catalogService.incrementVersion()
        catalogService.refreshCache()
        logger.info("Service rejected: {} by {} — reason: {}", serviceId, reviewedBy, reason)
    }

    fun mergeService(
        serviceId: String,
        mergeIntoServiceId: String,
        reason: String,
        reviewedBy: String,
    ) {
        val service =
            catalogRepository.findServiceById(serviceId)
                ?: throw IllegalArgumentException("Service not found: $serviceId")
        require(service.status == CatalogServiceStatus.PENDING_REVIEW) {
            "Only pending_review services can be merged. Current status: ${service.status}"
        }
        val target =
            catalogRepository.findServiceById(mergeIntoServiceId)
                ?: throw IllegalArgumentException("Target service not found: $mergeIntoServiceId")
        require(target.status == CatalogServiceStatus.ACTIVE) {
            "Can only merge into active services. Target status: ${target.status}"
        }
        catalogRepository.migrateProfileServices(serviceId, mergeIntoServiceId)
        catalogRepository.updateServiceStatus(serviceId, CatalogServiceStatus.MERGED, reason, reviewedBy, mergeIntoServiceId)
        catalogService.incrementVersion()
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
