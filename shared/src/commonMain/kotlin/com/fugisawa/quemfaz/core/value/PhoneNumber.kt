package com.fugisawa.quemfaz.core.value

import com.fugisawa.quemfaz.core.validation.ValidationResult
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a phone number in E.164 format or Brazilian national format.
 * Examples: "+5511987654321" or "11987654321"
 */
@Serializable
@JvmInline
value class PhoneNumber private constructor(val value: String) {
    companion object {
        /**
         * Creates a PhoneNumber after validation.
         * Accepts formats: +5511987654321, 5511987654321, 11987654321
         */
        fun create(value: String): ValidationResult<PhoneNumber> {
            val cleaned = value.trim().replace(Regex("[\\s\\-().]"), "")

            return when {
                cleaned.isEmpty() ->
                    ValidationResult.invalid("phoneNumber", "Phone number cannot be empty")

                cleaned.length < 10 || cleaned.length > 15 ->
                    ValidationResult.invalid("phoneNumber", "Phone number must be between 10 and 15 digits")

                !cleaned.all { it.isDigit() || it == '+' } ->
                    ValidationResult.invalid("phoneNumber", "Phone number can only contain digits and optional leading +")

                cleaned.startsWith("+") && !cleaned.drop(1).all { it.isDigit() } ->
                    ValidationResult.invalid("phoneNumber", "Invalid phone number format")

                else -> ValidationResult.valid(PhoneNumber(cleaned))
            }
        }

        /**
         * Creates a PhoneNumber without validation.
         * Use only when the value is already validated (e.g., from database).
         */
        fun unsafe(value: String): PhoneNumber = PhoneNumber(value)
    }
}

/**
 * Represents a WhatsApp-enabled phone number.
 * Same validation as PhoneNumber but semantically indicates WhatsApp availability.
 */
@Serializable
@JvmInline
value class WhatsAppPhone private constructor(val value: String) {
    companion object {
        fun create(value: String): ValidationResult<WhatsAppPhone> {
            val cleaned = value.trim().replace(Regex("[\\s\\-().]"), "")

            return when {
                cleaned.isEmpty() ->
                    ValidationResult.invalid("whatsAppPhone", "WhatsApp phone cannot be empty")

                cleaned.length < 10 || cleaned.length > 15 ->
                    ValidationResult.invalid("whatsAppPhone", "WhatsApp phone must be between 10 and 15 digits")

                !cleaned.all { it.isDigit() || it == '+' } ->
                    ValidationResult.invalid("whatsAppPhone", "WhatsApp phone can only contain digits and optional leading +")

                cleaned.startsWith("+") && !cleaned.drop(1).all { it.isDigit() } ->
                    ValidationResult.invalid("whatsAppPhone", "Invalid WhatsApp phone format")

                else -> ValidationResult.valid(WhatsAppPhone(cleaned))
            }
        }

        fun unsafe(value: String): WhatsAppPhone = WhatsAppPhone(value)
    }

    /**
     * Converts this WhatsApp phone to a regular PhoneNumber.
     */
    fun toPhoneNumber(): PhoneNumber = PhoneNumber.unsafe(value)
}
