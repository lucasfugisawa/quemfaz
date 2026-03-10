package com.fugisawa.quemfaz.contract.moderation

import com.fugisawa.quemfaz.domain.moderation.ReportReason
import com.fugisawa.quemfaz.domain.moderation.ReportTargetType
import kotlinx.serialization.Serializable

@Serializable
data class CreateReportRequest(
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val description: String? = null
)

@Serializable
data class ReportResponse(
    val id: String,
    val reporterUserId: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val description: String?,
    val status: String,
    val createdAt: String,
    val resolvedAt: String? = null,
    val resolutionAction: String? = null
)

@Serializable
data class ReportListResponse(
    val reports: List<ReportResponse>
)
