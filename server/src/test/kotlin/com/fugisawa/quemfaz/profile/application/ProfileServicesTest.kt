package com.fugisawa.quemfaz.profile.application

import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentity
import com.fugisawa.quemfaz.auth.domain.UserPhoneAuthIdentityRepository
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.catalog.application.CatalogService
import com.fugisawa.quemfaz.city.application.CityService
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.junit.Test
import org.mockito.kotlin.mock
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

        override fun listPublishedByCity(cityId: String) = profiles.values.filter { it.cityId == cityId }

        override fun search(
            serviceIds: List<String>,
            cityId: String?,
        ): List<ProfessionalProfile> = emptyList()

        override fun updateStatus(
            id: ProfessionalProfileId,
            status: ProfessionalProfileStatus,
        ): Boolean {
            val p = profiles[id.value] ?: return false
            profiles[id.value] = p.copy(status = status)
            return true
        }

        override fun updateKnownName(
            id: ProfessionalProfileId,
            knownName: String?,
        ): Boolean {
            val p = profiles[id.value] ?: return false
            profiles[id.value] = p.copy(knownName = knownName)
            return true
        }

        override fun incrementViewCount(id: ProfessionalProfileId) {
            val p = profiles[id.value] ?: return
            profiles[id.value] = p.copy(viewCount = p.viewCount + 1)
        }

        override fun incrementContactClickCount(id: ProfessionalProfileId) {
            val p = profiles[id.value] ?: return
            profiles[id.value] = p.copy(contactClickCount = p.contactClickCount + 1)
        }

        override fun updateLastActiveAt(id: ProfessionalProfileId) {
            val p = profiles[id.value] ?: return
            profiles[id.value] = p.copy(lastActiveAt = Instant.now())
        }
    }

    private class FakeUserRepository : UserRepository {
        val users = mutableMapOf<String, User>()

        override fun create(user: User) = user.also { users[it.id.value] = it }

        override fun findById(id: UserId) = users[id.value]

        override fun updateName(
            id: UserId,
            fullName: String,
        ): User? {
            val u = users[id.value] ?: return null
            val updated = u.copy(fullName = fullName)
            users[id.value] = updated
            return updated
        }

        override fun updateDateOfBirth(
            id: UserId,
            dateOfBirth: java.time.LocalDate,
        ): User? {
            val u = users[id.value] ?: return null
            val updated = u.copy(dateOfBirth = dateOfBirth)
            users[id.value] = updated
            return updated
        }

        override fun updatePhotoUrl(
            id: UserId,
            photoUrl: String,
        ): User? {
            val u = users[id.value] ?: return null
            val updated = u.copy(photoUrl = photoUrl)
            users[id.value] = updated
            return updated
        }

        override fun updateStatus(
            id: UserId,
            status: UserStatus,
        ): Boolean {
            val u = users[id.value] ?: return false
            users[id.value] = u.copy(status = status)
            return true
        }

        override fun acceptTerms(
            id: UserId,
            termsVersion: String,
            privacyVersion: String,
        ): User? = null
    }

    private class FakeUserPhoneAuthIdentityRepository : UserPhoneAuthIdentityRepository {
        val identities = mutableMapOf<String, UserPhoneAuthIdentity>()

        override fun findByPhoneNumber(phoneNumber: String) = identities.values.find { it.phoneNumber == phoneNumber }

        override fun findByUserId(userId: UserId) = identities.values.find { it.userId == userId }

        override fun create(identity: UserPhoneAuthIdentity) = identity.also { identities[it.id] = it }

        override fun markVerified(
            id: String,
            verifiedAt: Instant,
        ): Boolean {
            val i = identities[id] ?: return false
            identities[id] = i.copy(isVerified = true, verifiedAt = verifiedAt)
            return true
        }
    }

    @Test
    fun `should confirm and publish professional profile`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val phoneAuthRepo = FakeUserPhoneAuthIdentityRepository()
        val userId = UserId("user-123")
        userRepo.create(
            User(
                userId,
                "John Doe",
                "https://example.com/photo.jpg",
                UserStatus.ACTIVE,
                dateOfBirth = java.time.LocalDate.of(1990, 1, 1),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        phoneAuthRepo.create(UserPhoneAuthIdentity("id-123", userId, "+5516999999999", true, Instant.now(), Instant.now(), Instant.now()))

        val service = ConfirmProfessionalProfileService(profileRepo, userRepo, mock(), phoneAuthRepo, mock())
        val request =
            ConfirmProfessionalProfileRequest(
                description = "Pintor experiente",
                selectedServiceIds = listOf("paint-residential"),
                cityId = "batatais",
                portfolioPhotoUrls = emptyList(),
            )

        val response = service.execute(userId, request)

        assertNotNull(response.id)
        assertEquals("John Doe", response.fullName)
        assertEquals("Pintor experiente", response.description)
        assertEquals(ProfileCompleteness.COMPLETE, profileRepo.findByUserId(userId)?.completeness)
        assertEquals(1, response.services.size)
        assertEquals("paint-residential", response.services[0].serviceId)
    }

    @Test
    fun `should update existing professional profile`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val phoneAuthRepo = FakeUserPhoneAuthIdentityRepository()
        val userId = UserId("user-123")
        userRepo.create(
            User(
                userId,
                "John Doe",
                "https://example.com/photo.jpg",
                UserStatus.ACTIVE,
                dateOfBirth = java.time.LocalDate.of(1990, 1, 1),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        phoneAuthRepo.create(UserPhoneAuthIdentity("id-123", userId, "+5516999999999", true, Instant.now(), Instant.now(), Instant.now()))

        // Create initial profile via confirm service
        val confirmService = ConfirmProfessionalProfileService(profileRepo, userRepo, mock(), phoneAuthRepo, mock())
        confirmService.execute(
            userId,
            ConfirmProfessionalProfileRequest(
                description = "Pintor experiente",
                selectedServiceIds = listOf("paint-residential"),
                cityId = "batatais",
                portfolioPhotoUrls = emptyList(),
            ),
        )

        val updateService = UpdateProfessionalProfileService(profileRepo, userRepo, mock(), phoneAuthRepo, mock())
        val result =
            updateService.execute(
                userId,
                ConfirmProfessionalProfileRequest(
                    description = "Pintor e eletricista",
                    selectedServiceIds = listOf("paint-residential", "electrical-residential"),
                    cityId = "batatais",
                    portfolioPhotoUrls = emptyList(),
                ),
            )

        assertIs<UpdateProfileResult.Success>(result)
        val response = (result as UpdateProfileResult.Success).response
        assertEquals("Pintor e eletricista", response.description)
        assertEquals(2, response.services.size)
        assertEquals(true, response.profileComplete)

        // Profile id should be the same (upsert)
        assertEquals(1, profileRepo.profiles.size)
    }

    @Test
    fun `should return NotFound when updating profile that does not exist`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val phoneAuthRepo = FakeUserPhoneAuthIdentityRepository()
        val userId = UserId("user-no-profile")
        userRepo.create(User(userId, "Jane", null, UserStatus.ACTIVE, createdAt = Instant.now(), updatedAt = Instant.now()))

        val service = UpdateProfessionalProfileService(profileRepo, userRepo, mock(), phoneAuthRepo, mock())
        val result =
            service.execute(
                userId,
                ConfirmProfessionalProfileRequest(
                    description = "Desc",
                    selectedServiceIds = listOf("paint-residential"),
                    cityId = "batatais",
                    portfolioPhotoUrls = emptyList(),
                ),
            )

        assertIs<UpdateProfileResult.NotFound>(result)
    }

    @Test
    fun `should return Blocked when updating a blocked profile`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val phoneAuthRepo = FakeUserPhoneAuthIdentityRepository()
        val userId = UserId("user-blocked")
        userRepo.create(User(userId, "Blocked User", null, UserStatus.ACTIVE, createdAt = Instant.now(), updatedAt = Instant.now()))

        // Create profile directly as BLOCKED
        val profileId = ProfessionalProfileId("prof-blocked")
        profileRepo.save(
            ProfessionalProfile(
                profileId,
                userId,
                null,
                "Desc",
                "Desc",
                "batatais",
                emptyList(),
                emptyList(),
                ProfileCompleteness.INCOMPLETE,
                ProfessionalProfileStatus.BLOCKED,
                Instant.now(),
                Instant.now(),
                Instant.now(),
            ),
        )

        val service = UpdateProfessionalProfileService(profileRepo, userRepo, mock(), phoneAuthRepo, mock())
        val result =
            service.execute(
                userId,
                ConfirmProfessionalProfileRequest(
                    description = "New desc",
                    selectedServiceIds = listOf("paint-residential"),
                    cityId = "batatais",
                    portfolioPhotoUrls = emptyList(),
                ),
            )

        assertIs<UpdateProfileResult.Blocked>(result)
    }

    @Test
    fun `should get public profile only if published`() {
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val phoneAuthRepo = FakeUserPhoneAuthIdentityRepository()
        val userId = UserId("user-123")
        val profileId = ProfessionalProfileId("prof-123")

        userRepo.create(User(userId, "John Doe", null, UserStatus.ACTIVE, createdAt = Instant.now(), updatedAt = Instant.now()))

        val service = GetPublicProfessionalProfileService(profileRepo, userRepo, mock(), phoneAuthRepo, mock())

        // No profile yet
        assertEquals(null, service.execute(profileId))

        // Save a draft
        val draft =
            ProfessionalProfile(
                profileId,
                userId,
                null,
                "Desc",
                "Desc",
                "batatais",
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
