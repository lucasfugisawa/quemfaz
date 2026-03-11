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

/**
 * Represents a photo in a professional's portfolio.
 *
 * @property url The URL of the photo
 * @property caption Optional description of the photo
 * @property order Display order (0-indexed), lower values appear first
 */
@Serializable
data class PortfolioPhoto(
    val url: PhotoUrl,
    val caption: String?,
    val order: Int = 0
)

/**
 * Represents a professional profile with all details.
 *
 * Business rules:
 * - Published profiles must be complete
 * - Description is the original user input
 * - NormalizedDescription is cleaned/standardized for search
 * - Portfolio photos are ordered by the `order` field
 *
 * @property description Original user-provided description of services
 * @property normalizedDescription Cleaned and standardized description for search indexing
 */
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
) {
    init {
        // Business rule: Published profiles must be complete
        require(status != ProfileStatus.PUBLISHED || completeness == ProfileCompleteness.COMPLETE) {
            "Published profiles must be complete"
        }

        // Validate neighborhoods
        require(neighborhoods.isNotEmpty() || status == ProfileStatus.DRAFT) {
            "Published profiles must have at least one neighborhood"
        }

        // Validate services
        require(services.isNotEmpty() || status == ProfileStatus.DRAFT) {
            "Published profiles must have at least one service"
        }

        // Validate description
        require(description.isNotBlank() || status == ProfileStatus.DRAFT) {
            "Published profiles must have a description"
        }
    }

    /**
     * Returns portfolio photos sorted by their display order.
     */
    val orderedPortfolioPhotos: List<PortfolioPhoto>
        get() = portfolioPhotos.sortedBy { it.order }

    /**
     * Returns true if this profile can be published (meets all requirements).
     */
    val canBePublished: Boolean
        get() = completeness == ProfileCompleteness.COMPLETE &&
                services.isNotEmpty() &&
                neighborhoods.isNotEmpty() &&
                description.isNotBlank()

    /**
     * Returns missing fields that prevent publication.
     */
    fun getMissingFields(): List<MissingProfileField> = buildList {
        if (services.isEmpty()) add(MissingProfileField.SERVICES)
        if (neighborhoods.isEmpty()) add(MissingProfileField.NEIGHBORHOODS)
        if (description.isBlank()) add(MissingProfileField.DESCRIPTION)
    }
}

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
