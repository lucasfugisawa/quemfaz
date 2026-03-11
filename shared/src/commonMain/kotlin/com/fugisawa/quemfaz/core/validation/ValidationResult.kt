package com.fugisawa.quemfaz.core.validation

import com.fugisawa.quemfaz.core.error.AppError
import com.fugisawa.quemfaz.core.error.ValidationIssue
import com.fugisawa.quemfaz.core.result.AppResult

/**
 * Result type specifically for validation operations.
 * Can accumulate multiple validation issues.
 */
sealed interface ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>
    data class Invalid(val issues: List<ValidationIssue>) : ValidationResult<Nothing> {
        constructor(field: String, message: String) : this(listOf(ValidationIssue(field, message)))
    }

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid

    companion object {
        fun <T> valid(value: T): ValidationResult<T> = Valid(value)
        fun <T> invalid(field: String, message: String): ValidationResult<T> = Invalid(field, message)
        fun <T> invalid(issues: List<ValidationIssue>): ValidationResult<T> = Invalid(issues)
    }
}

/**
 * Converts a ValidationResult to an AppResult.
 */
fun <T> ValidationResult<T>.toAppResult(): AppResult<T> = when (this) {
    is ValidationResult.Valid -> AppResult.success(value)
    is ValidationResult.Invalid -> AppResult.failure(AppError.ValidationError(issues = issues))
}

/**
 * Maps the value if valid, returns invalid unchanged.
 */
inline fun <T, R> ValidationResult<T>.map(transform: (T) -> R): ValidationResult<R> = when (this) {
    is ValidationResult.Valid -> ValidationResult.valid(transform(value))
    is ValidationResult.Invalid -> this
}

/**
 * Flat-maps the value if valid, returns invalid unchanged.
 */
inline fun <T, R> ValidationResult<T>.flatMap(transform: (T) -> ValidationResult<R>): ValidationResult<R> = when (this) {
    is ValidationResult.Valid -> transform(value)
    is ValidationResult.Invalid -> this
}

/**
 * Combines multiple validation results.
 * Returns Valid only if all results are valid, otherwise accumulates all issues.
 */
fun <T> List<ValidationResult<T>>.combine(): ValidationResult<List<T>> {
    val allIssues = mutableListOf<ValidationIssue>()
    val values = mutableListOf<T>()

    forEach { result ->
        when (result) {
            is ValidationResult.Valid -> values.add(result.value)
            is ValidationResult.Invalid -> allIssues.addAll(result.issues)
        }
    }

    return if (allIssues.isEmpty()) {
        ValidationResult.valid(values)
    } else {
        ValidationResult.invalid(allIssues)
    }
}

/**
 * Combines two validation results using the provided transform.
 * Returns Invalid if either result is invalid, accumulating all issues.
 */
inline fun <T1, T2, R> ValidationResult<T1>.zip(
    other: ValidationResult<T2>,
    transform: (T1, T2) -> R
): ValidationResult<R> {
    return when {
        this is ValidationResult.Valid && other is ValidationResult.Valid ->
            ValidationResult.valid(transform(this.value, other.value))
        this is ValidationResult.Invalid && other is ValidationResult.Invalid ->
            ValidationResult.invalid(this.issues + other.issues)
        this is ValidationResult.Invalid -> this
        other is ValidationResult.Invalid -> other
        else -> error("Unreachable")
    }
}
