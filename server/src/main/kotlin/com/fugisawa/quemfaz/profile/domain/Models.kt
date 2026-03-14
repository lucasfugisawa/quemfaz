package com.fugisawa.quemfaz.profile.domain

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import java.time.Instant

data class ProfessionalProfile(
    val id: ProfessionalProfileId,
    val userId: UserId,
    val knownName: String?,
    val description: String?,
    val normalizedDescription: String?,
    val contactPhone: String?,
    val whatsappPhone: String?,
    val cityName: String?,
    val neighborhoods: List<String>,
    val services: List<ProfessionalProfileService>,
    val portfolioPhotos: List<PortfolioPhoto>,
    val completeness: ProfileCompleteness,
    val status: ProfessionalProfileStatus,
    val lastActiveAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ProfessionalProfileService(
    val serviceId: String,
    val matchLevel: ServiceMatchLevel,
)

data class PortfolioPhoto(
    val id: String,
    val photoUrl: String,
    val caption: String?,
    val createdAt: Instant,
)

enum class ProfileCompleteness {
    INCOMPLETE,
    COMPLETE,
}

enum class ProfessionalProfileStatus {
    DRAFT,
    PUBLISHED,
    BLOCKED,
}

interface ProfessionalProfileRepository {
    fun findByUserId(userId: UserId): ProfessionalProfile?

    fun findById(id: ProfessionalProfileId): ProfessionalProfile?

    fun save(profile: ProfessionalProfile): ProfessionalProfile

    fun listPublishedByCity(cityName: String): List<ProfessionalProfile>
    fun search(serviceIds: List<String>, cityName: String?): List<ProfessionalProfile>

    fun updateStatus(
        id: ProfessionalProfileId,
        status: ProfessionalProfileStatus,
    ): Boolean

    fun updateKnownName(id: ProfessionalProfileId, knownName: String?): Boolean
}
