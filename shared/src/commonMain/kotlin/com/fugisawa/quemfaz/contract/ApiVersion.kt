package com.fugisawa.quemfaz.contract

/**
 * API versioning constants.
 * Update CURRENT when making breaking changes to the API contract.
 */
object ApiVersion {
    /**
     * Current API version. Follows semantic versioning.
     * Format: MAJOR.MINOR
     * - MAJOR: Incremented for breaking changes
     * - MINOR: Incremented for backward-compatible additions
     */
    const val CURRENT = "1.0"

    /**
     * Minimum supported API version.
     * Clients with versions below this should be prompted to update.
     */
    const val MIN_SUPPORTED = "1.0"

    /**
     * Checks if a given version is supported.
     */
    fun isSupported(version: String): Boolean {
        val (clientMajor, _) = parseVersion(version)
        val (minMajor, _) = parseVersion(MIN_SUPPORTED)
        return clientMajor >= minMajor
    }

    private fun parseVersion(version: String): Pair<Int, Int> {
        val parts = version.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return major to minor
    }
}
