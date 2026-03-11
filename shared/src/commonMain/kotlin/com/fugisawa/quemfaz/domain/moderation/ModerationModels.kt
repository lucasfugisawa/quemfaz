package com.fugisawa.quemfaz.domain.moderation

import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Reasons why a user might report content or another user.
 */
@Serializable
enum class ReportReason {
    SPAM,
    INAPPROPRIATE_CONTENT,
    WRONG_PHONE_NUMBER,
    FAKE_PROFILE,
    ABUSIVE_BEHAVIOR,
    OTHER
}

/**
 * Types of entities that can be reported.
 * Currently only professional profiles, but extensible for future needs.
 */
@Serializable
enum class ReportTargetType {
    PROFESSIONAL_PROFILE
}

/**
 * Scope of a moderation block action.
 */
@Serializable
enum class BlockType {
    /** Block a specific professional profile only */
    PROFILE,
    /** Block the entire user account */
    USER
}

/**
 * Actions that can be taken by moderators on reported content.
 */
@Serializable
enum class ModerationAction {
    BLOCK_PROFILE,
    BLOCK_USER,
    REMOVE_CONTENT,
    DISMISS_REPORT
}

/**
 * A user-submitted report flagging problematic content or behavior.
 *
 * @property id Unique report identifier
 * @property reporterUserId User who submitted the report
 * @property targetType What type of entity was reported
 * @property targetId ID of the reported entity (e.g., profile ID)
 * @property reason Category of the complaint
 * @property description Optional detailed explanation from the reporter
 * @property createdAt When the report was submitted
 */
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
