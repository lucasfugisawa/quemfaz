package com.fugisawa.quemfaz.moderation.application

import com.fugisawa.quemfaz.contract.moderation.ReportListResponse
import com.fugisawa.quemfaz.contract.moderation.ReportResponse
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.moderation.domain.*
import com.fugisawa.quemfaz.profile.domain.*
import com.fugisawa.quemfaz.auth.domain.*
import org.slf4j.LoggerFactory
import java.time.Instant

class ModerationService(
    private val reportRepository: ReportRepository,
    private val profileRepository: ProfessionalProfileRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun listReports(status: ReportStatus? = null): ReportListResponse {
        val reports = reportRepository.list(status)
        return ReportListResponse(reports.map { mapToResponse(it) })
    }

    fun getReportDetails(reportId: ReportId): ReportResponse? {
        return reportRepository.findById(reportId)?.let { mapToResponse(it) }
    }

    fun dismissReport(reportId: ReportId) {
        val report = reportRepository.findById(reportId) ?: throw IllegalArgumentException("Report not found")
        val updatedReport = report.copy(
            status = ReportStatus.DISMISSED,
            resolvedAt = Instant.now(),
            resolutionAction = "DISMISSED"
        )
        reportRepository.save(updatedReport)
        logger.info("Report ${reportId.value} dismissed")
    }

    fun blockProfessionalProfile(profileId: ProfessionalProfileId, reportId: ReportId? = null) {
        val updated = profileRepository.updateStatus(profileId, ProfessionalProfileStatus.BLOCKED)
        if (!updated) throw IllegalArgumentException("Profile not found")

        reportId?.let { rid ->
            reportRepository.findById(rid)?.let { report ->
                val updatedReport = report.copy(
                    status = ReportStatus.RESOLVED,
                    resolvedAt = Instant.now(),
                    resolutionAction = "BLOCKED_PROFILE"
                )
                reportRepository.save(updatedReport)
            }
        }
        logger.info("Professional profile ${profileId.value} blocked")
    }

    fun blockUser(userId: UserId, reportId: ReportId? = null) {
        val updated = userRepository.updateStatus(userId, UserStatus.BLOCKED)
        if (!updated) throw IllegalArgumentException("User not found")

        // Also block their professional profile if it exists
        profileRepository.findByUserId(userId)?.let { profile ->
            profileRepository.updateStatus(profile.id, ProfessionalProfileStatus.BLOCKED)
            logger.info("Professional profile ${profile.id.value} blocked due to user block")
        }

        reportId?.let { rid ->
            reportRepository.findById(rid)?.let { report ->
                val updatedReport = report.copy(
                    status = ReportStatus.RESOLVED,
                    resolvedAt = Instant.now(),
                    resolutionAction = "BLOCKED_USER"
                )
                reportRepository.save(updatedReport)
            }
        }
        logger.info("User ${userId.value} blocked")
    }

    private fun mapToResponse(report: ServerReport) = ReportResponse(
        id = report.id.value,
        reporterUserId = report.reporterUserId.value,
        targetType = report.targetType,
        targetId = report.targetId,
        reason = report.reason,
        description = report.description,
        status = report.status.name,
        createdAt = report.createdAt.toString(),
        resolvedAt = report.resolvedAt?.toString(),
        resolutionAction = report.resolutionAction
    )
}
