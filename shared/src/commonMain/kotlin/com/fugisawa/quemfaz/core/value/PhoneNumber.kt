package com.fugisawa.quemfaz.core.value

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PhoneNumber(val value: String) {
    fun isLikelyBrazilianPhoneNumber(): Boolean {
        // Very conservative MVP check: at least 10 digits (DDD + number)
        // Brazilian numbers are typically 55 (country) + 2 digits (DDD) + 8 or 9 digits
        val digits = value.filter { it.isDigit() }
        return digits.length >= 10
    }
}
