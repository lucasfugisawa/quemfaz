package com.fugisawa.quemfaz.domain.moderation

import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ReportReason {
    SPAM,
    INAPPROPRIATE_CONTENT,
    WRONG_PHONE_NUMBER,
    FAKE_PROFILE,
    ABUSIVE_BEHAVIOR,
    OTHER
}

@Serializable
enum class ReportTargetType {
    PROFESSIONAL_PROFILE
}

@Serializable
enum class BlockType {
    PROFILE,
    USER
}

@Serializable
enum class ModerationAction {
    BLOCK_PROFILE,
    BLOCK_USER,
    REMOVE_CONTENT,
    DISMISS_REPORT
}

@Serializable
data class Report(
    val id: ReportId,
    val reporterUserId: UserId,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val description: String?,
    val createdAt: Instant
)
