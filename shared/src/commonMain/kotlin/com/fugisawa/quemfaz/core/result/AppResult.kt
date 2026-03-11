package com.fugisawa.quemfaz.core.result

import com.fugisawa.quemfaz.core.error.AppError
import kotlinx.serialization.Serializable

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T]
 * or a failure with an [AppError].
 *
 * This is the primary result type for operations that can fail with domain-specific errors.
 */
@Serializable
sealed interface AppResult<out T> {
    /**
     * Represents a successful operation with a value.
     */
    @Serializable
    data class Success<T>(val value: T) : AppResult<T>

    /**
     * Represents a failed operation with an error.
     */
    @Serializable
    data class Failure(val error: AppError) : AppResult<Nothing>

    /**
     * Returns `true` if this result represents a success.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns `true` if this result represents a failure.
     */
    val isFailure: Boolean get() = this is Failure

    companion object {
        /**
         * Creates a successful result wrapping the given [value].
         */
        fun <T> success(value: T): AppResult<T> = Success(value)

        /**
         * Creates a failed result with the given [error].
         */
        fun <T> failure(error: AppError): AppResult<T> = Failure(error)
    }
}

/**
 * Returns the encapsulated value if this result is a success, or `null` if it is a failure.
 */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> null
}

/**
 * Returns the encapsulated value if this result is a success, or throws an exception if it is a failure.
 */
fun <T> AppResult<T>.getOrThrow(): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> throw IllegalStateException("AppResult is Failure: ${error.message}")
}

/**
 * Returns the encapsulated value if this result is a success,
 * or the result of [onFailure] function if it is a failure.
 */
inline fun <T> AppResult<T>.getOrElse(onFailure: (AppError) -> T): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> onFailure(error)
}

/**
 * Returns the encapsulated value if this result is a success, or a default value if it is a failure.
 */
fun <T> AppResult<T>.getOrDefault(defaultValue: T): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Failure -> defaultValue
}

/**
 * Returns the encapsulated error if this result is a failure, or `null` if it is a success.
 */
fun <T> AppResult<T>.errorOrNull(): AppError? = when (this) {
    is AppResult.Success -> null
    is AppResult.Failure -> error
}

/**
 * Performs the given [action] on the encapsulated value if this result is a success.
 * Returns the original result unchanged.
 */
inline fun <T> AppResult<T>.onSuccess(action: (value: T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

/**
 * Performs the given [action] on the encapsulated error if this result is a failure.
 * Returns the original result unchanged.
 */
inline fun <T> AppResult<T>.onFailure(action: (error: AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

/**
 * Maps the encapsulated value if this result is a success, or returns the failure unchanged.
 */
inline fun <T, R> AppResult<T>.map(transform: (value: T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.success(transform(value))
    is AppResult.Failure -> this
}

/**
 * Maps the encapsulated error if this result is a failure, or returns the success unchanged.
 */
inline fun <T> AppResult<T>.mapError(transform: (error: AppError) -> AppError): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.failure(transform(error))
}

/**
 * Flat-maps the encapsulated value if this result is a success, or returns the failure unchanged.
 * Use this when the transform function itself returns an AppResult.
 */
inline fun <T, R> AppResult<T>.flatMap(transform: (value: T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}

/**
 * Returns the result of [onSuccess] for success or [onFailure] for failure.
 * This is the general fold operation that handles both cases.
 */
inline fun <T, R> AppResult<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (error: AppError) -> R
): R = when (this) {
    is AppResult.Success -> onSuccess(value)
    is AppResult.Failure -> onFailure(error)
}

/**
 * Recovers from a failure by applying the [recovery] function to the error.
 * Returns the original success unchanged.
 */
inline fun <T> AppResult<T>.recover(recovery: (error: AppError) -> T): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.success(recovery(error))
}

/**
 * Recovers from a failure by applying the [recovery] function to the error,
 * which itself returns an AppResult. Returns the original success unchanged.
 */
inline fun <T> AppResult<T>.recoverWith(recovery: (error: AppError) -> AppResult<T>): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> recovery(error)
}

/**
 * Combines two AppResults using the provided [transform] function.
 * Returns failure if either result is a failure.
 */
inline fun <T1, T2, R> AppResult<T1>.zip(
    other: AppResult<T2>,
    transform: (T1, T2) -> R
): AppResult<R> = when {
    this is AppResult.Success && other is AppResult.Success -> AppResult.success(transform(this.value, other.value))
    this is AppResult.Failure -> this
    other is AppResult.Failure -> other
    else -> error("Unreachable")
}

/**
 * Converts a nullable value to an AppResult, using the provided [error] for null values.
 */
fun <T : Any> T?.toAppResult(error: AppError): AppResult<T> =
    if (this != null) AppResult.success(this) else AppResult.failure(error)

/**
 * Converts a standard Kotlin Result to an AppResult, mapping exceptions to UnexpectedError.
 */
fun <T> Result<T>.toAppResult(mapException: (Throwable) -> AppError): AppResult<T> =
    fold(
        onSuccess = { AppResult.success(it) },
        onFailure = { AppResult.failure(mapException(it)) }
    )
