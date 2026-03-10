package com.fugisawa.quemfaz.search.interpretation

import com.fugisawa.quemfaz.search.domain.InterpretedSearchQuery

interface SearchQueryInterpreter {
    fun interpret(
        query: String,
        cityContext: String?,
    ): InterpretedSearchQuery
}
