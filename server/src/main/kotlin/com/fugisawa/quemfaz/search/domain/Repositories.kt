package com.fugisawa.quemfaz.search.domain

interface SearchQueryRepository {
    fun create(searchQuery: SearchQuery): SearchQuery
}
