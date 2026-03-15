package com.fugisawa.quemfaz.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LlmConfigTest {
    @Test
    fun `LlmConfig has sensible defaults`() {
        val config = LlmConfig()
        assertEquals(8000L, config.timeoutMs)
    }

    @Test
    fun `LlmConfig accepts custom timeout`() {
        val config = LlmConfig(timeoutMs = 5000L)
        assertEquals(5000L, config.timeoutMs)
    }
}
