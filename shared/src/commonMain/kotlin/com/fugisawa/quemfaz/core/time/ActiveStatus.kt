package com.fugisawa.quemfaz.core.time

import kotlinx.serialization.Serializable

/**
 * Represents the recent activity status of a professional profile.
 *
 * This is used to indicate whether a professional has been active on the platform
 * within a recent time window (e.g., last 30 days).
 *
 * @property ACTIVE_RECENTLY Profile owner has been active within the recent time window
 * @property INACTIVE Profile owner has not been active recently
 */
@Serializable
enum class ActiveStatus {
    /** Professional has been active within the recent time window (typically last 30 days) */
    ACTIVE_RECENTLY,

    /** Professional has not been active recently */
    INACTIVE
}
