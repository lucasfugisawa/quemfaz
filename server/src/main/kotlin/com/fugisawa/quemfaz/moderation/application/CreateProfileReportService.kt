package com.fugisawa.quemfaz.moderation.application

import com.fugisawa.quemfaz.contract.moderation.CreateReportRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.moderation.ReportTargetType
import com.fugisawa.quemfaz.moderation.domain.ReportRepository
import com.fugisawa.quemfaz.moderation.domain.ReportStatus
import com.fugisawa.quemfaz.moderation.domain.ServerReport
import com.fugisawa.quemfaz.profile.domain.ProfessionalProfileRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class CreateProfileReportService(
    private val reportRepository: ReportRepository,
    private val profileRepository: ProfessionalProfileRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(
        reporterId: UserId,
        request: CreateReportRequest,
    ): ServerReport {
        if (request.targetType != ReportTargetType.PROFESSIONAL_PROFILE) {
            throw IllegalArgumentException("Only professional profiles can be reported in MVP")
        }

        val profileId = ProfessionalProfileId(request.targetId)
        profileRepository.findById(profileId)
            ?: throw IllegalArgumentException("Target profile not found")

        val report =
            ServerReport(
                id = ReportId(UUID.randomUUID().toString()),
                reporterUserId = reporterId,
                targetType = request.targetType,
                targetId = request.targetId,
                reason = request.reason,
                description = request.description,
                status = ReportStatus.OPEN,
                createdAt = Instant.now(),
                resolvedAt = null,
                resolutionAction = null,
            )

        reportRepository.save(report)
        logger.info("Report ${report.id.value} created by user ${reporterId.value} for profile ${profileId.value}")
        return report
    }
}
