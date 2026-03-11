package com.fugisawa.quemfaz.core.value

import com.fugisawa.quemfaz.core.validation.ValidationResult
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Represents a person's name.
 * Must be between 2 and 100 characters and contain only letters, spaces, and common name characters.
 */
@Serializable
@JvmInline
value class PersonName private constructor(val value: String) {
    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 100
        private val VALID_NAME_REGEX = Regex("^[\\p{L}\\s'-]+$")

        fun create(value: String): ValidationResult<PersonName> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() ->
                    ValidationResult.invalid("name", "Name cannot be empty")

                trimmed.length < MIN_LENGTH ->
                    ValidationResult.invalid("name", "Name must be at least $MIN_LENGTH characters")

                trimmed.length > MAX_LENGTH ->
                    ValidationResult.invalid("name", "Name cannot exceed $MAX_LENGTH characters")

                !VALID_NAME_REGEX.matches(trimmed) ->
                    ValidationResult.invalid("name", "Name can only contain letters, spaces, hyphens and apostrophes")

                else -> ValidationResult.valid(PersonName(trimmed))
            }
        }

        fun unsafe(value: String): PersonName = PersonName(value)
    }
}

/**
 * Represents a photo URL.
 * Must be a valid HTTP(S) URL.
 */
@Serializable
@JvmInline
value class PhotoUrl private constructor(val value: String) {
    companion object {
        private val URL_REGEX = Regex("^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+$")

        fun create(value: String): ValidationResult<PhotoUrl> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() ->
                    ValidationResult.invalid("photoUrl", "Photo URL cannot be empty")

                !URL_REGEX.matches(trimmed) ->
                    ValidationResult.invalid("photoUrl", "Photo URL must be a valid HTTP(S) URL")

                else -> ValidationResult.valid(PhotoUrl(trimmed))
            }
        }

        fun unsafe(value: String): PhotoUrl = PhotoUrl(value)
    }
}

/**
 * Represents a neighborhood name.
 * Must be between 2 and 100 characters.
 */
@Serializable
@JvmInline
value class NeighborhoodName private constructor(val value: String) {
    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 100

        fun create(value: String): ValidationResult<NeighborhoodName> {
            val trimmed = value.trim()

            return when {
                trimmed.isEmpty() ->
                    ValidationResult.invalid("neighborhood", "Neighborhood name cannot be empty")

                trimmed.length < MIN_LENGTH ->
                    ValidationResult.invalid("neighborhood", "Neighborhood name must be at least $MIN_LENGTH characters")

                trimmed.length > MAX_LENGTH ->
                    ValidationResult.invalid("neighborhood", "Neighborhood name cannot exceed $MAX_LENGTH characters")

                else -> ValidationResult.valid(NeighborhoodName(trimmed))
            }
        }

        fun unsafe(value: String): NeighborhoodName = NeighborhoodName(value)
    }
}
