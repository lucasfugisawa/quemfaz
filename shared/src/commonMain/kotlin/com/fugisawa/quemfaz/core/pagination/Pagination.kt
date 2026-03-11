package com.fugisawa.quemfaz.core.pagination

import kotlinx.serialization.Serializable

/**
 * Represents a request for a page of data.
 * Validates that limit and offset are within acceptable bounds.
 *
 * @property limit Maximum number of items to return (1-100)
 * @property offset Number of items to skip (must be non-negative)
 */
@Serializable
data class PageRequest(
    val limit: Int,
    val offset: Int
) {
    init {
        require(limit > 0) { "Limit must be positive, got: $limit" }
        require(limit <= MAX_LIMIT) { "Limit cannot exceed $MAX_LIMIT, got: $limit" }
        require(offset >= 0) { "Offset cannot be negative, got: $offset" }
    }

    companion object {
        const val MAX_LIMIT = 100
        const val DEFAULT_LIMIT = 20
        const val DEFAULT_OFFSET = 0

        /**
         * Creates a PageRequest with default values.
         */
        fun default(): PageRequest = PageRequest(DEFAULT_LIMIT, DEFAULT_OFFSET)

        /**
         * Creates the first page with the given limit.
         */
        fun firstPage(limit: Int = DEFAULT_LIMIT): PageRequest = PageRequest(limit, 0)

        /**
         * Creates the next page based on the current page.
         */
        fun nextPage(current: PageRequest): PageRequest =
            PageRequest(current.limit, current.offset + current.limit)
    }

    /**
     * Calculates the page number (1-indexed) from offset and limit.
     */
    val pageNumber: Int get() = (offset / limit) + 1

    /**
     * Returns true if this is the first page.
     */
    val isFirstPage: Boolean get() = offset == 0
}

/**
 * Represents a page of data with metadata.
 *
 * @property items The items in this page
 * @property limit The maximum number of items per page
 * @property offset The number of items skipped
 * @property total The total number of items across all pages
 */
@Serializable
data class Page<T>(
    val items: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Long
) {
    /**
     * Returns true if there are more pages available.
     */
    val hasMore: Boolean get() = offset + items.size < total

    /**
     * Returns the total number of pages.
     */
    val totalPages: Int get() = if (limit > 0) ((total + limit - 1) / limit).toInt() else 0

    /**
     * Returns the current page number (1-indexed).
     */
    val pageNumber: Int get() = if (limit > 0) (offset / limit) + 1 else 0

    /**
     * Returns true if this is the last page.
     */
    val isLastPage: Boolean get() = !hasMore

    /**
     * Returns true if this is the first page.
     */
    val isFirstPage: Boolean get() = offset == 0

    companion object {
        /**
         * Creates an empty page.
         */
        fun <T> empty(request: PageRequest): Page<T> =
            Page(emptyList(), request.limit, request.offset, 0)
    }
}

/**
 * Maps the items in a page using the provided transform function.
 */
inline fun <T, R> Page<T>.map(transform: (T) -> R): Page<R> =
    Page(items.map(transform), limit, offset, total)
