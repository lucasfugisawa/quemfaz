package com.fugisawa.quemfaz.core.pagination

import kotlinx.serialization.Serializable

@Serializable
data class PageRequest(
    val limit: Int,
    val offset: Int
)

@Serializable
data class Page<T>(
    val items: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Long
)
