package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.catalog.application.CatalogEntry
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.catalog.domain.CatalogServiceStatus
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.core.id.CityId
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.city.City
import com.fugisawa.quemfaz.domain.service.ServiceMatchLevel
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileService
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class ProfileResponseMapperTest {

    private val batatais = City(
        id = CityId("batatais"), name = "Batatais", stateCode = "SP",
        country = "BR", latitude = -20.89, longitude = -47.58, isActive = true,
    )

    private val fakeCityRepo = object : CityRepository {
        override fun findById(id: String) = if (id == "batatais") batatais else null
        override fun findByName(name: String) = if (name.equals("Batatais", ignoreCase = true)) batatais else null
        override fun listActive() = listOf(batatais)
    }

    private val cityService = CityService(fakeCityRepo)

    private val pendingEntry = CatalogEntry(
        id = "svc-pending", displayName = "Pending Service", description = "",
        aliases = emptyList(), categoryId = "cat", status = CatalogServiceStatus.PENDING_REVIEW,
    )
    private val activeEntry = CatalogEntry(
        id = "svc-active", displayName = "Active Service", description = "",
        aliases = emptyList(), categoryId = "cat", status = CatalogServiceStatus.ACTIVE,
    )

    private val catalogService: CatalogService = mock<CatalogService>().also {
        whenever(it.findById("svc-pending")).thenReturn(pendingEntry)
        whenever(it.findById("svc-active")).thenReturn(activeEntry)
    }

    private val mapper = ProfileResponseMapper(catalogService, cityService)

    private fun buildProfile(
        services: List<ProfessionalProfileService> = listOf(
            ProfessionalProfileService("svc-pending", ServiceMatchLevel.PRIMARY),
            ProfessionalProfileService("svc-active", ServiceMatchLevel.SECONDARY),
        ),
    ) = ProfessionalProfile(
        id = ProfessionalProfileId("prof-1"),
        userId = UserId("user-1"),
        knownName = "Ana",
        description = "desc",
        normalizedDescription = "desc",
        cityId = "batatais",
        services = services,
        portfolioPhotos = emptyList(),
        completeness = ProfileCompleteness.COMPLETE,
        status = ProfessionalProfileStatus.PUBLISHED,
        lastActiveAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test
    fun `toResponse with includeServiceStatus true shows pending_review`() {
        val response = mapper.toResponse(buildProfile(), "Ana Silva", "photo.jpg", "11999", includeServiceStatus = true)
        assertEquals("prof-1", response.id)
        assertEquals("Ana Silva", response.fullName)
        assertEquals("batatais", response.cityId)
        assertEquals("Batatais", response.cityName)
        val pendingSvc = response.services.first { it.serviceId == "svc-pending" }
        assertEquals("pending_review", pendingSvc.status)
        val activeSvc = response.services.first { it.serviceId == "svc-active" }
        assertEquals("active", activeSvc.status)
    }

    @Test
    fun `toResponse with includeServiceStatus false always shows active`() {
        val response = mapper.toResponse(buildProfile(), "Ana Silva", "photo.jpg", "11999", includeServiceStatus = false)
        val pendingSvc = response.services.first { it.serviceId == "svc-pending" }
        assertEquals("active", pendingSvc.status)
    }

    @Test
    fun `toResponse resolves city name from cityId`() {
        val response = mapper.toResponse(buildProfile(), "Ana", null, "")
        assertEquals("Batatais", response.cityName)
    }

    @Test
    fun `toResponse returns empty cityName for unknown cityId`() {
        val profile = buildProfile().copy(cityId = "unknown")
        val response = mapper.toResponse(profile, "Ana", null, "")
        assertEquals("", response.cityName)
    }
}
