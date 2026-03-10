package com.fugisawa.quemfaz.auth.domain

object PhoneNormalizer {
    fun normalize(phoneNumber: String): String {
        // Simple normalization for Brazilian numbers for now.
        // Remove all non-digit characters.
        val digits = phoneNumber.filter { it.isDigit() }

        // If it starts with 55 and has 12 or 13 digits, it's already got the country code.
        // If it has 10 or 11 digits, add 55.
        return when (digits.length) {
            10, 11 -> "55$digits"
            12, 13 -> digits
            else -> digits // Fallback
        }
    }
}
