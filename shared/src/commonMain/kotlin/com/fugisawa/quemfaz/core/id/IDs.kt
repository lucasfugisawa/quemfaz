package com.fugisawa.quemfaz.core.id

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class UserId(val value: String)

@Serializable
@JvmInline
value class ProfessionalProfileId(val value: String)

@Serializable
@JvmInline
value class CityId(val value: String)

@Serializable
@JvmInline
value class ReportId(val value: String)

@Serializable
@JvmInline
value class SearchQueryId(val value: String)

@Serializable
@JvmInline
value class FavoriteId(val value: String)

@Serializable
@JvmInline
value class CanonicalServiceId(val value: String)
