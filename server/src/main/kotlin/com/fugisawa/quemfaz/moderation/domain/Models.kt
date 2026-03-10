package com.fugisawa.quemfaz.moderation.domain

import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.moderation.*
import java.time.Instant

enum class ReportStatus {
    OPEN,
    DISMISSED,
    RESOLVED
}

data class ServerReport(
    val id: ReportId,
    val reporterUserId: UserId,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val description: String?,
    val status: ReportStatus,
    val createdAt: Instant,
    val resolvedAt: Instant?,
    val resolutionAction: String?
)

interface ReportRepository {
    fun save(report: ServerReport)
    fun findById(id: ReportId): ServerReport?
    fun list(status: ReportStatus? = null): List<ServerReport>
}
