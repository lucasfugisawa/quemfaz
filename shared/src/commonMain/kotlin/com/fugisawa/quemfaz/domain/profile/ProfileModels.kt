package com.fugisawa.quemfaz.domain.profile

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.core.time.ActiveStatus
import com.fugisawa.quemfaz.core.value.NeighborhoodName
import com.fugisawa.quemfaz.core.value.PhoneNumber
import com.fugisawa.quemfaz.core.value.PhotoUrl
import com.fugisawa.quemfaz.core.value.WhatsAppPhone
import com.fugisawa.quemfaz.domain.city.City
import com.fugisawa.quemfaz.domain.service.ProfessionalService
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ProfileStatus {
    DRAFT,
    PUBLISHED,
    BLOCKED
}

@Serializable
enum class ProfileCompleteness {
    INCOMPLETE,
    COMPLETE
}

@Serializable
enum class MissingProfileField {
    SERVICES,
    CITY,
    NEIGHBORHOODS,
    CONTACT_PHONE,
    DESCRIPTION
}

@Serializable
data class PortfolioPhoto(
    val url: PhotoUrl,
    val caption: String?
)

@Serializable
data class ProfessionalProfile(
    val id: ProfessionalProfileId,
    val userId: UserId,
    val description: String,
    val normalizedDescription: String,
    val contactPhone: PhoneNumber,
    val whatsAppPhone: WhatsAppPhone?,
    val city: City,
    val neighborhoods: List<NeighborhoodName>,
    val services: List<ProfessionalService>,
    val portfolioPhotos: List<PortfolioPhoto>,
    val completeness: ProfileCompleteness,
    val activeStatus: ActiveStatus,
    val status: ProfileStatus,
    val lastActiveAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class ProfessionalProfileDraft(
    val originalInput: String,
    val normalizedDescription: String,
    val suggestedServices: List<ProfessionalService>,
    val suggestedCityName: String?,
    val suggestedNeighborhoods: List<String>,
    val freeTextAliases: List<String>,
    val missingFields: List<MissingProfileField>,
    val followUpQuestions: List<String>
)
