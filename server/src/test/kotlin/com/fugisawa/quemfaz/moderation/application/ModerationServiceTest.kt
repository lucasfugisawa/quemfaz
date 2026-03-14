package com.fugisawa.quemfaz.moderation.application

import com.fugisawa.quemfaz.auth.domain.User
import com.fugisawa.quemfaz.auth.domain.UserRepository
import com.fugisawa.quemfaz.auth.domain.UserStatus
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.moderation.domain.ReportRepository
import com.fugisawa.quemfaz.moderation.domain.ReportStatus
import com.fugisawa.quemfaz.moderation.domain.ServerReport
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfile
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileStatus
import com.fugisawa.quemfaz.profile.domain.ProfileCompleteness
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class ModerationServiceTest {
    private class FakeReportRepository : ReportRepository {
        val reports = mutableMapOf<String, ServerReport>()

        override fun save(report: ServerReport) {
            reports[report.id.value] = report
        }

        override fun findById(id: ReportId) = reports[id.value]

        override fun list(status: ReportStatus?) = reports.values.toList()
    }

    private class FakeProfessionalProfileRepository : ProfessionalProfileRepository {
        val profiles = mutableMapOf<String, ProfessionalProfile>()

        override fun findByUserId(userId: UserId) = profiles.values.find { it.userId == userId }

        override fun findById(id: ProfessionalProfileId) = profiles[id.value]

        override fun save(profile: ProfessionalProfile): ProfessionalProfile {
            profiles[profile.id.value] = profile
            return profile
        }

        override fun listPublishedByCity(cityName: String) = profiles.values.filter { it.status == ProfessionalProfileStatus.PUBLISHED }

        override fun search(
            serviceIds: List<String>,
            cityName: String?,
        ): List<ProfessionalProfile> = emptyList()

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
        ): User? = null

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
    fun `should block professional profile and resolve report`() {
        val reportRepo = FakeReportRepository()
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val moderationService = ModerationService(reportRepo, profileRepo, userRepo)

        val profileId = ProfessionalProfileId("prof-1")
        profileRepo.save(createProfile(profileId, UserId("u1"), ProfessionalProfileStatus.PUBLISHED))

        val reportId = ReportId("rep-1")
        reportRepo.save(
            ServerReport(
                reportId,
                UserId("u2"),
                com.fugisawa.quemfaz.domain.moderation.ReportTargetType.PROFESSIONAL_PROFILE,
                profileId.value,
                com.fugisawa.quemfaz.domain.moderation.ReportReason.SPAM,
                null,
                ReportStatus.OPEN,
                Instant.now(),
                null,
                null,
            ),
        )

        moderationService.blockProfessionalProfile(profileId, reportId)

        assertEquals(ProfessionalProfileStatus.BLOCKED, profileRepo.findById(profileId)?.status)
        val resolvedReport = reportRepo.findById(reportId)
        assertEquals(ReportStatus.RESOLVED, resolvedReport?.status)
        assertEquals("BLOCKED_PROFILE", resolvedReport?.resolutionAction)
    }

    @Test
    fun `should block user and their profile`() {
        val reportRepo = FakeReportRepository()
        val profileRepo = FakeProfessionalProfileRepository()
        val userRepo = FakeUserRepository()
        val moderationService = ModerationService(reportRepo, profileRepo, userRepo)

        val userId = UserId("user-1")
        val profileId = ProfessionalProfileId("prof-1")
        userRepo.create(User(userId, "John", null, UserStatus.ACTIVE, Instant.now(), Instant.now()))
        profileRepo.save(createProfile(profileId, userId, ProfessionalProfileStatus.PUBLISHED))

        moderationService.blockUser(userId)

        assertEquals(UserStatus.BLOCKED, userRepo.findById(userId)?.status)
        assertEquals(ProfessionalProfileStatus.BLOCKED, profileRepo.findById(profileId)?.status)
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
