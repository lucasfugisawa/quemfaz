package com.fugisawa.quemfaz.core.id

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe wrappers for entity identifiers.
 *
 * Using value classes provides compile-time type safety (preventing mixing of ID types)
 * with zero runtime overhead (inlined to underlying String).
 */

/** Unique identifier for a user account */
@Serializable
@JvmInline
value class UserId(val value: String)

/** Unique identifier for a professional service profile */
@Serializable
@JvmInline
value class ProfessionalProfileId(val value: String)

/** Unique identifier for a city */
@Serializable
@JvmInline
value class CityId(val value: String)

/** Unique identifier for a moderation report */
@Serializable
@JvmInline
value class ReportId(val value: String)

/** Unique identifier for a search query (for analytics) */
@Serializable
@JvmInline
value class SearchQueryId(val value: String)

/** Unique identifier for a user's favorite/saved profile */
@Serializable
@JvmInline
value class FavoriteId(val value: String)

/** Unique identifier for a canonical service type */
@Serializable
@JvmInline
value class CanonicalServiceId(val value: String)
