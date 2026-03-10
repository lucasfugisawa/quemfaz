package com.fugisawa.quemfaz.core.error

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppError {
    val code: String
    val message: String

    @Serializable
    data class ValidationError(
        override val code: String = "VALIDATION_ERROR",
        override val message: String = "Validation failed",
        val issues: List<ValidationIssue>
    ) : AppError

    @Serializable
    data class NotFoundError(
        override val code: String = "NOT_FOUND",
        override val message: String
    ) : AppError

    @Serializable
    data class ConflictError(
        override val code: String = "CONFLICT",
        override val message: String
    ) : AppError

    @Serializable
    data class UnauthorizedError(
        override val code: String = "UNAUTHORIZED",
        override val message: String = "User is not authenticated"
    ) : AppError

    @Serializable
    data class ForbiddenError(
        override val code: String = "FORBIDDEN",
        override val message: String = "User does not have permission"
    ) : AppError

    @Serializable
    data class BlockedError(
        override val code: String = "BLOCKED",
        override val message: String = "User or profile is blocked"
    ) : AppError

    @Serializable
    data class UnexpectedError(
        override val code: String = "UNEXPECTED_ERROR",
        override val message: String = "An unexpected error occurred"
    ) : AppError
}
