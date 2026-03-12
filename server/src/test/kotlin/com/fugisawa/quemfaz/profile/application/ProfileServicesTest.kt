package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ProfileServicesTest {
    private class FakeProfessionalProfileRepository : ProfessionalProfileRepository {
        val profiles = mutableMapOf<String, ProfessionalProfile>()

        override fun findByUserId(userId: UserId) = profiles.values.find { it.userId == userId }

        override fun findById(id: ProfessionalProfileId) = profiles[id.value]

        override fun save(profile: ProfessionalProfile): ProfessionalProfile {
            profiles[profile.id.value] = profile
            return profile
        }

        override fun listPublishedByCity(cityName: String) = profiles.values.filter { it.cityName == cityName }

        override fun updateStatus(
            id: ProfessionalProfileId,
            status: ProfessionalProfileStatus,
        ): Boolean {
            val p = profiles[id.value] ?: return false
            profiles[id.value] = p.copy(status = status)
            return true
        }
    }

    private class FakeUserRepository : UserRepository {
        val users = mutableMapOf<String, User>()

        override fun create(user: User) = user.also { users[it.id.value] = it }

        override fun findById(id: UserId) = users[id.value]

        override fun updateProfile(
            id: UserId,
            name: String,
            photoUrl: String?,
        ) = null

        override fun updateStatus(
            id: UserId,
            status: UserStatus,
        ): Boolean {
            val u = users[id.value] ?: return false
            users[id.value] = u.copy(status = status)
            return true
        }
    }

    @Test
    fun `should confirm and publish professional profile`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val userId = UserId("user-123")
        userRepo.create(User(userId, "John Doe", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))

        val service = ConfirmProfessionalProfileService(profileRepo, userRepo)
        val request =
            ConfirmProfessionalProfileRequest(
                normalizedDescription = "Pintor experiente",
                selectedServiceIds = listOf("paint-residential"),
                cityName = "Batatais",
                neighborhoods = listOf("Centro"),
                contactPhone = "16999999999",
                whatsAppPhone = "16999999999",
                photoUrl = null,
                portfolioPhotoUrls = emptyList(),
            )

        val response = service.execute(userId, request)

        assertNotNull(response.id)
        assertEquals("John Doe", response.name)
        assertEquals("Pintor experiente", response.description)
        assertEquals(ProfileCompleteness.COMPLETE, profileRepo.findByUserId(userId)?.completeness)
        assertEquals(1, response.services.size)
        assertEquals("paint-residential", response.services[0].serviceId)
    }

    @Test
    fun `should update existing professional profile`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val userId = UserId("user-123")
        userRepo.create(User(userId, "John Doe", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))

        // Create initial profile via confirm service
        val confirmService = ConfirmProfessionalProfileService(profileRepo, userRepo)
        confirmService.execute(
            userId,
            ConfirmProfessionalProfileRequest(
                normalizedDescription = "Pintor experiente",
                selectedServiceIds = listOf("paint-residential"),
                cityName = "Batatais",
                neighborhoods = listOf("Centro"),
                contactPhone = "16999999999",
                whatsAppPhone = null,
                photoUrl = null,
                portfolioPhotoUrls = emptyList(),
            ),
        )

        val updateService = UpdateProfessionalProfileService(profileRepo, userRepo)
        val result =
            updateService.execute(
                userId,
                ConfirmProfessionalProfileRequest(
                    normalizedDescription = "Pintor e eletricista",
                    selectedServiceIds = listOf("paint-residential", "electrical-residential"),
                    cityName = "Batatais",
                    neighborhoods = listOf("Centro", "Vila Nova"),
                    contactPhone = "16988888888",
                    whatsAppPhone = "16988888888",
                    photoUrl = null,
                    portfolioPhotoUrls = emptyList(),
                ),
            )

        assertIs<UpdateProfileResult.Success>(result)
        val response = (result as UpdateProfileResult.Success).response
        assertEquals("Pintor e eletricista", response.description)
        assertEquals(2, response.services.size)
        assertEquals(listOf("Centro", "Vila Nova"), response.neighborhoods)
        assertEquals("16988888888", response.contactPhone)
        assertEquals("16988888888", response.whatsAppPhone)
        assertEquals(true, response.profileComplete)

        // Profile id should be the same (upsert)
        assertEquals(1, profileRepo.profiles.size)
    }

    @Test
    fun `should return NotFound when updating profile that does not exist`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val userId = UserId("user-no-profile")
        userRepo.create(User(userId, "Jane", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))

        val service = UpdateProfessionalProfileService(profileRepo, userRepo)
        val result =
            service.execute(
                userId,
                ConfirmProfessionalProfileRequest(
                    normalizedDescription = "Desc",
                    selectedServiceIds = listOf("paint-residential"),
                    cityName = "Batatais",
                    neighborhoods = emptyList(),
                    contactPhone = "16999999999",
                    whatsAppPhone = null,
                    photoUrl = null,
                    portfolioPhotoUrls = emptyList(),
                ),
            )

        assertIs<UpdateProfileResult.NotFound>(result)
    }

    @Test
    fun `should return Blocked when updating a blocked profile`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val userId = UserId("user-blocked")
        userRepo.create(User(userId, "Blocked User", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))

        // Create profile directly as BLOCKED
        val profileId = ProfessionalProfileId("prof-blocked")
        profileRepo.save(
            ProfessionalProfile(
                profileId,
                userId,
                "Desc",
                "Desc",
                "123",
                null,
                "City",
                emptyList(),
                emptyList(),
                emptyList(),
                ProfileCompleteness.INCOMPLETE,
                ProfessionalProfileStatus.BLOCKED,
                Instant.now(),
                Instant.now(),
                Instant.now(),
            ),
        )

        val service = UpdateProfessionalProfileService(profileRepo, userRepo)
        val result =
            service.execute(
                userId,
                ConfirmProfessionalProfileRequest(
                    normalizedDescription = "New desc",
                    selectedServiceIds = listOf("paint-residential"),
                    cityName = "Batatais",
                    neighborhoods = emptyList(),
                    contactPhone = "16999999999",
                    whatsAppPhone = null,
                    photoUrl = null,
                    portfolioPhotoUrls = emptyList(),
                ),
            )

        assertIs<UpdateProfileResult.Blocked>(result)
    }

    @Test
    fun `should get public profile only if published`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val userId = UserId("user-123")
        val profileId = ProfessionalProfileId("prof-123")

        userRepo.create(User(userId, "John Doe", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))

        val service = GetPublicProfessionalProfileService(profileRepo, userRepo)

        // No profile yet
        assertEquals(null, service.execute(profileId))

        // Save a draft
        val draft =
            ProfessionalProfile(
                profileId,
                userId,
                "Desc",
                "Desc",
                "123",
                "123",
                "City",
                emptyList(),
                emptyList(),
                emptyList(),
                ProfileCompleteness.INCOMPLETE,
                ProfessionalProfileStatus.DRAFT,
                Instant.now(),
                Instant.now(),
                Instant.now(),
            )
        profileRepo.save(draft)
        assertEquals(null, service.execute(profileId))

        // Publish it
        profileRepo.save(draft.copy(status = ProfessionalProfileStatus.PUBLISHED))
        assertNotNull(service.execute(profileId))
    }
}
