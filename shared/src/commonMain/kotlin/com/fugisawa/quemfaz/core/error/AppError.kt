package com.fugisawa.quemfaz.core.error

import kotlinx.serialization.Serializable

/**
 * Base interface for all application errors.
 * Provides a consistent error structure for API responses and client-side error handling.
 */
@Serializable
sealed interface AppError {
    val code: String
    val message: String
    val details: Map<String, String>?

    @Serializable
    data class ValidationError(
        override val code: String = ErrorCodes.VALIDATION_ERROR,
        override val message: String = ErrorMessages.VALIDATION_FAILED,
        val issues: List<ValidationIssue>,
        override val details: Map<String, String>? = null
    ) : AppError

    @Serializable
    data class NotFoundError(
        override val code: String = ErrorCodes.NOT_FOUND,
        override val message: String,
        override val details: Map<String, String>? = null
    ) : AppError

    @Serializable
    data class ConflictError(
        override val code: String = ErrorCodes.CONFLICT,
        override val message: String,
        override val details: Map<String, String>? = null
    ) : AppError

    @Serializable
    data class UnauthorizedError(
        override val code: String = ErrorCodes.UNAUTHORIZED,
        override val message: String = ErrorMessages.NOT_AUTHENTICATED,
        override val details: Map<String, String>? = null
    ) : AppError

    @Serializable
    data class ForbiddenError(
        override val code: String = ErrorCodes.FORBIDDEN,
        override val message: String = ErrorMessages.INSUFFICIENT_PERMISSIONS,
        override val details: Map<String, String>? = null
    ) : AppError

    @Serializable
    data class BlockedError(
        override val code: String = ErrorCodes.BLOCKED,
        override val message: String = ErrorMessages.USER_OR_PROFILE_BLOCKED,
        override val details: Map<String, String>? = null
    ) : AppError

    @Serializable
    data class UnexpectedError(
        override val code: String = ErrorCodes.UNEXPECTED_ERROR,
        override val message: String = ErrorMessages.UNEXPECTED_ERROR_OCCURRED,
        override val details: Map<String, String>? = null
    ) : AppError
}
