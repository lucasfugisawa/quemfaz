package com.fugisawa.quemfaz.llm

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.structure.executeStructured
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory

open class LlmAgentService(
    @PublishedApi internal val promptExecutor: PromptExecutor,
    private val timeoutMs: Long = 8000L,
) {
    private val logger = LoggerFactory.getLogger(LlmAgentService::class.java)

    constructor(timeoutMs: Long = 8000L) : this(createExecutor(), timeoutMs)

    open suspend fun <T> executeStructured(
        systemPrompt: String,
        userMessage: String,
        serializer: KSerializer<T>,
    ): T {
        val result =
            withTimeout(timeoutMs) {
                promptExecutor.executeStructured(
                    prompt =
                        prompt("quemfaz-structured") {
                            system(systemPrompt)
                            user(userMessage)
                        },
                    model = OpenAIModels.Chat.GPT4oMini,
                    serializer = serializer,
                )
            }
        return result.getOrThrow().data
    }

    suspend inline fun <reified T> executeStructured(
        systemPrompt: String,
        userMessage: String,
    ): T = executeStructured(systemPrompt, userMessage, serializer<T>())

    companion object {
        private fun createExecutor(): PromptExecutor {
            val apiKey = System.getenv("OPENAI_API_KEY")
            if (apiKey.isNullOrBlank()) {
                LoggerFactory
                    .getLogger(LlmAgentService::class.java)
                    .warn("OPENAI_API_KEY not set. LLM features will fail at runtime.")
            }
            return simpleOpenAIExecutor(apiKey ?: "")
        }
    }
}
