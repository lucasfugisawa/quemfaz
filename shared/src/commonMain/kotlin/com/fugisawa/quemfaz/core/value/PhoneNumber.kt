package com.fugisawa.quemfaz.core.value

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PhoneNumber(val value: String)
