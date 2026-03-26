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

/**
 * Holds the result of interpreting a search query.
 *
 * [cityId] is a general city identifier: interpreters set it to a city **name**
 * (the LLM output), while [SearchProfessionalsService] may construct a copy with
 * a resolved city **ID** for ranking. The service layer bridges the two.
 */
data class InterpretedSearchQuery(
    val originalQuery: String,
    val normalizedQuery: String,
    val serviceIds: List<String>,
    val cityId: String?,
    val freeTextAliases: List<String>,
    val llmUnavailable: Boolean = false,
    val blockedDescriptions: List<String> = emptyList(),
)
