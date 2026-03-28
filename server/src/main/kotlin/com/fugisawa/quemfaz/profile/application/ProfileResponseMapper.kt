package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.profile.InterpretedServiceDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProfileResponseMapper(
    private val catalogService: CatalogService,
    private val cityService: CityService,
) {
    fun toResponse(
        profile: ProfessionalProfile,
        fullName: String,
        userPhotoUrl: String?,
        phone: String,
        includeServiceStatus: Boolean = false,
    ): ProfessionalProfileResponse =
        ProfessionalProfileResponse(
            id = profile.id.value,
            fullName = fullName,
            knownName = profile.knownName,
            photoUrl = userPhotoUrl ?: profile.portfolioPhotos.firstOrNull()?.photoUrl,
            description = profile.description ?: "",
            cityId = profile.cityId ?: "",
            cityName = cityService.resolveNameFromId(profile.cityId) ?: "",
            services =
                profile.services.map { svc ->
                    val canonical = catalogService.findById(svc.serviceId)
                    val status =
                        if (includeServiceStatus) {
                            when (canonical?.status) {
                                CatalogServiceStatus.PENDING_REVIEW -> "pending_review"
                                else -> "active"
                            }
                        } else {
                            "active"
                        }
                    InterpretedServiceDto(
                        svc.serviceId,
                        canonical?.displayName ?: svc.serviceId,
                        svc.matchLevel.name,
                        status = status,
                    )
                },
            profileComplete = profile.completeness == ProfileCompleteness.COMPLETE,
            activeRecently = profile.lastActiveAt.isAfter(Instant.now().minusSeconds(86400 * 7)),
            phone = phone,
            portfolioPhotoUrls = profile.portfolioPhotos.map { it.photoUrl },
            contactCount = profile.contactClickCount,
            daysSinceActive = ChronoUnit.DAYS.between(profile.lastActiveAt, Instant.now()).toInt(),
            status = profile.status.name.lowercase(),
        )
}
