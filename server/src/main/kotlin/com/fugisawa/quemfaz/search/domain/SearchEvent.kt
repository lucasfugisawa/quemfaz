package com.fugisawa.quemfaz.search.domain

import java.time.Instant

data class SearchEvent(
    val id: String,
    val resolvedServiceId: String,
    val cityName: String,
    val createdAt: Instant,
)

data class PopularServiceResult(
    val serviceId: String,
    val displayName: String,
    val count: Long,
)

interface SearchEventRepository {
    fun logEvents(events: List<SearchEvent>)
    fun getPopularServices(cityName: String?, limit: Int, windowDays: Int): List<PopularServiceResult>
    fun countSearchesInWindow(cityName: String, windowDays: Int): Long
}
