package com.fugisawa.quemfaz.core.time

import kotlinx.serialization.Serializable

@Serializable
enum class ActiveStatus {
    ACTIVE_RECENTLY,
    INACTIVE
}
