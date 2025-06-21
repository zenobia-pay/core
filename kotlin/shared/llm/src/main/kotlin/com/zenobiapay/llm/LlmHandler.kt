package com.zenobiapay.llm

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest

private val logger = KotlinLogging.logger {}

class LlmHandler @Inject constructor(
    private val objectMapper: ObjectMapper
) {

    private val bedrockClient: BedrockRuntimeClient by lazy {
        BedrockRuntimeClient.builder().build()
    }

    fun generateResponse(
        prompt: String,
        systemPrompt: String,
        modelId: String,
        temperature: Double
    ): String {
        try {
            val requestBody = when {
                modelId.startsWith("anthropic.claude") -> createClaudeRequest(prompt, systemPrompt, temperature)
                else -> throw IllegalArgumentException("Unsupported model ID: $modelId")
            }

            val request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(requestBody)
                .build()

            val response = bedrockClient.invokeModel(request)
            val responseBody = response.body().asUtf8String()

            return parseModelResponse(responseBody, modelId)
        } catch (e: Exception) {
            logger.error(e) { "Error generating LLM response" }
            throw RuntimeException("Failed to generate LLM response", e)
        }
    }

    private fun createClaudeRequest(prompt: String, systemPrompt: String, temperature: Double): software.amazon.awssdk.core.SdkBytes {
        val requestJson = objectMapper.createObjectNode().apply {
            put("anthropic_version", "bedrock-2023-05-31")
            put("temperature", temperature)
            put("max_tokens", 4000)  // Setting a large max token limit

            putArray("messages").apply {
                // Claude on Bedrock only supports 'user' and 'assistant' roles, not 'system'
                // So we'll prepend the system prompt to the user message
                addObject().apply {
                    put("role", "user")
                    put("content", "$systemPrompt\n\n$prompt")
                }
            }
        }

        return software.amazon.awssdk.core.SdkBytes.fromUtf8String(requestJson.toString())
    }

    private fun parseModelResponse(responseBody: String, modelId: String): String {
        return try {
            val jsonNode = objectMapper.readTree(responseBody)

            when {
                modelId.startsWith("anthropic.claude") -> {
                    val content = jsonNode.path("content")
                    if (content.isArray && content.size() > 0) {
                        content[0].path("text").asText()
                    } else {
                        ""
                    }
                }
                modelId.startsWith("amazon.titan") -> {
                    jsonNode.path("results").path(0).path("outputText").asText()
                }
                else -> {
                    logger.warn { "Unknown model format, returning raw response" }
                    responseBody
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse model response" }
            "Error parsing response: ${e.message}"
        }
    }
}