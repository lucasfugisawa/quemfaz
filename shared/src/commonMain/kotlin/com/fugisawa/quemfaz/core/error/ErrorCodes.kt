package com.fugisawa.quemfaz.core.error

/**
 * Centralized error codes for the application.
 * These codes are used in API responses and for client-side error handling.
 */
object ErrorCodes {
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val BLOCKED = "BLOCKED"
    const val UNEXPECTED_ERROR = "UNEXPECTED_ERROR"
}

/**
 * Centralized error messages for the application.
 * These provide default user-facing messages for each error type.
 */
object ErrorMessages {
    const val VALIDATION_FAILED = "Validation failed"
    const val RESOURCE_NOT_FOUND = "Resource not found"
    const val RESOURCE_CONFLICT = "Resource already exists"
    const val NOT_AUTHENTICATED = "User is not authenticated"
    const val INSUFFICIENT_PERMISSIONS = "User does not have permission"
    const val USER_OR_PROFILE_BLOCKED = "User or profile is blocked"
    const val UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred"
}
