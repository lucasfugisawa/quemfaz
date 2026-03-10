package com.fugisawa.quemfaz.favorites.application

import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.favorites.domain.Favorite
import com.fugisawa.quemfaz.favorites.domain.FavoriteRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.junit.Test
import java.time.Instant
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteServicesTest {
    private class FakeFavoriteRepository : FavoriteRepository {
        val favorites = mutableListOf<Favorite>()

        override fun add(favorite: Favorite) {
            favorites.add(favorite)
        }

        override fun remove(
            userId: UserId,
            professionalProfileId: ProfessionalProfileId,
        ) {
            favorites.removeIf { it.userId == userId && it.professionalProfileId == professionalProfileId }
        }

        override fun exists(
            userId: UserId,
            professionalProfileId: ProfessionalProfileId,
        ) = favorites.any { it.userId == userId && it.professionalProfileId == professionalProfileId }

        override fun listByUserId(userId: UserId) = favorites.filter { it.userId == userId }
    }

    private class FakeProfessionalProfileRepository : ProfessionalProfileRepository {
        val profiles = mutableMapOf<String, ProfessionalProfile>()

        override fun findByUserId(userId: UserId) = profiles.values.find { it.userId == userId }

        override fun findById(id: ProfessionalProfileId) = profiles[id.value]

        override fun save(profile: ProfessionalProfile): ProfessionalProfile {
            profiles[profile.id.value] = profile
            return profile
        }

        override fun listPublishedByCity(cityName: String) =
            profiles.values.filter {
                it.cityName == cityName &&
                    it.status == ProfessionalProfileStatus.PUBLISHED
            }

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
        override fun create(user: User) = user

        override fun findById(id: UserId): User? = null

        override fun updateProfile(
            id: UserId,
            name: String,
            photoUrl: String?,
        ): User? = null

        override fun updateStatus(
            id: UserId,
            status: UserStatus,
        ): Boolean = false
    }

    @Test
    fun `should add favorite successfully`() {
        val favoriteRepo = FakeFavoriteRepository()
        val profileRepo = FakeProfessionalProfileRepository()
        val userId = UserId("user-1")
        val profileId = ProfessionalProfileId("prof-1")

        profileRepo.save(createProfile(profileId, userId, ProfessionalProfileStatus.PUBLISHED))

        val service = AddFavoriteService(favoriteRepo, profileRepo)
        service.execute(userId, profileId)

        assertTrue(favoriteRepo.exists(userId, profileId))
    }

    @Test
    fun `should not favorite non-published profile`() {
        val favoriteRepo = FakeFavoriteRepository()
        val profileRepo = FakeProfessionalProfileRepository()
        val userId = UserId("user-1")
        val profileId = ProfessionalProfileId("prof-1")

        profileRepo.save(createProfile(profileId, userId, ProfessionalProfileStatus.DRAFT))

        val service = AddFavoriteService(favoriteRepo, profileRepo)
        assertFailsWith<IllegalStateException> {
            service.execute(userId, profileId)
        }
    }

    @Test
    fun `should remove favorite`() {
        val favoriteRepo = FakeFavoriteRepository()
        val userId = UserId("user-1")
        val profileId = ProfessionalProfileId("prof-1")
        favoriteRepo.add(
            Favorite(
                com.fugisawa.quemfaz.core.id
                    .FavoriteId("f1"),
                userId,
                profileId,
                Instant.now(),
            ),
        )

        val service = RemoveFavoriteService(favoriteRepo)
        service.execute(userId, profileId)

        assertFalse(favoriteRepo.exists(userId, profileId))
    }

    private fun createProfile(
        id: ProfessionalProfileId,
        userId: UserId,
        status: ProfessionalProfileStatus,
    ) = ProfessionalProfile(
        id,
        userId,
        "Desc",
        "Desc",
        "123",
        "123",
        "City",
        emptyList(),
        emptyList(),
        emptyList(),
        ProfileCompleteness.COMPLETE,
        status,
        Instant.now(),
        Instant.now(),
        Instant.now(),
    )
}
