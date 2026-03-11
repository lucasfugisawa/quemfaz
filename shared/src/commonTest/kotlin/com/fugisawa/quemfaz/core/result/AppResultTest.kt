package com.fugisawa.quemfaz.core.result

import com.fugisawa.quemfaz.core.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppResultTest {
    private val testError = AppError.NotFoundError(message = "Not found")

    @Test
    fun `success should create Success result`() {
        val result = AppResult.success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `failure should create Failure result`() {
        val result = AppResult.failure<Int>(testError)
        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(testError, result.errorOrNull())
    }

    @Test
    fun `getOrNull should return value for success`() {
        val result = AppResult.success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `getOrNull should return null for failure`() {
        val result = AppResult.failure<Int>(testError)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrElse should return value for success`() {
        val result = AppResult.success(42)
        assertEquals(42, result.getOrElse { 0 })
    }

    @Test
    fun `getOrElse should return default for failure`() {
        val result = AppResult.failure<Int>(testError)
        assertEquals(0, result.getOrElse { 0 })
    }

    @Test
    fun `getOrDefault should return value for success`() {
        val result = AppResult.success(42)
        assertEquals(42, result.getOrDefault(0))
    }

    @Test
    fun `getOrDefault should return default for failure`() {
        val result = AppResult.failure<Int>(testError)
        assertEquals(0, result.getOrDefault(0))
    }

    @Test
    fun `map should transform success value`() {
        val result = AppResult.success(42)
        val mapped = result.map { it * 2 }
        assertEquals(84, mapped.getOrNull())
    }

    @Test
    fun `map should preserve failure`() {
        val result = AppResult.failure<Int>(testError)
        val mapped = result.map { it * 2 }
        assertTrue(mapped.isFailure)
        assertEquals(testError, mapped.errorOrNull())
    }

    @Test
    fun `flatMap should chain successful operations`() {
        val result = AppResult.success(42)
        val chained = result.flatMap { AppResult.success(it * 2) }
        assertEquals(84, chained.getOrNull())
    }

    @Test
    fun `flatMap should short-circuit on failure`() {
        val result = AppResult.failure<Int>(testError)
        val chained = result.flatMap { AppResult.success(it * 2) }
        assertTrue(chained.isFailure)
    }

    @Test
    fun `onSuccess should execute action for success`() {
        var executed = false
        AppResult.success(42).onSuccess { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `onSuccess should not execute action for failure`() {
        var executed = false
        AppResult.failure<Int>(testError).onSuccess { executed = true }
        assertFalse(executed)
    }

    @Test
    fun `onFailure should execute action for failure`() {
        var executed = false
        AppResult.failure<Int>(testError).onFailure { executed = true }
        assertTrue(executed)
    }

    @Test
    fun `onFailure should not execute action for success`() {
        var executed = false
        AppResult.success(42).onFailure { executed = true }
        assertFalse(executed)
    }

    @Test
    fun `fold should handle success case`() {
        val result = AppResult.success(42)
        val folded = result.fold(
            onSuccess = { it * 2 },
            onFailure = { 0 }
        )
        assertEquals(84, folded)
    }

    @Test
    fun `fold should handle failure case`() {
        val result = AppResult.failure<Int>(testError)
        val folded = result.fold(
            onSuccess = { it * 2 },
            onFailure = { 0 }
        )
        assertEquals(0, folded)
    }

    @Test
    fun `recover should handle failure`() {
        val result = AppResult.failure<Int>(testError)
        val recovered = result.recover { 42 }
        assertEquals(42, recovered.getOrNull())
    }

    @Test
    fun `recover should preserve success`() {
        val result = AppResult.success(84)
        val recovered = result.recover { 42 }
        assertEquals(84, recovered.getOrNull())
    }

    @Test
    fun `zip should combine two successes`() {
        val result1 = AppResult.success(2)
        val result2 = AppResult.success(3)
        val combined = result1.zip(result2) { a, b -> a * b }
        assertEquals(6, combined.getOrNull())
    }

    @Test
    fun `zip should fail if first fails`() {
        val result1 = AppResult.failure<Int>(testError)
        val result2 = AppResult.success(3)
        val combined = result1.zip(result2) { a, b -> a * b }
        assertTrue(combined.isFailure)
    }

    @Test
    fun `zip should fail if second fails`() {
        val result1 = AppResult.success(2)
        val result2 = AppResult.failure<Int>(testError)
        val combined = result1.zip(result2) { a, b -> a * b }
        assertTrue(combined.isFailure)
    }

    @Test
    fun `nullable to AppResult should handle non-null`() {
        val value = 42
        val result = value.toAppResult(testError)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `nullable to AppResult should handle null`() {
        val value: Int? = null
        val result = value.toAppResult(testError)
        assertTrue(result.isFailure)
        assertEquals(testError, result.errorOrNull())
    }
}
