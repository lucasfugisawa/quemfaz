package com.fugisawa.quemfaz.core.value

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PersonName(val value: String)

@Serializable
@JvmInline
value class PhotoUrl(val value: String)

@Serializable
@JvmInline
value class WhatsAppPhone(val value: String)

@Serializable
@JvmInline
value class NeighborhoodName(val value: String)
