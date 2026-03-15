package com.fugisawa.quemfaz.search.domain

import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.core.id.UserId
import java.time.Instant

data class SearchQuery(
    val id: String,
    val userId: UserId?,
    val originalQuery: String,
    val normalizedQuery: String,
    val cityName: String?,
    val interpretedServiceIds: List<String>,
    val inputMode: InputMode,
    val createdAt: Instant,
)

data class InterpretedSearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val serviceIds: List<String>,
    val cityName: String?,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
)
