package com.zenobiapay.webhook.util

import com.zenobiapay.webhook.di.ORUM_SLACK_WEBHOOK
import jakarta.inject.Inject
import jakarta.inject.Named
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class SlackChannel {
    ORUM
}

class SlackUtil @Inject constructor(
    private val client: OkHttpClient,
    @Named(ORUM_SLACK_WEBHOOK) private val orumSlackWebhook: String
) {
    fun sendMessage(message: String, slackChannel: SlackChannel) {
        val json = """
            { 
                "text": "$message"
            }
        """.trimIndent()
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(getWebhook(slackChannel))
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Slack webhook failed: ${response.code} - ${response.body?.string()}")
            }
        }
    }

    private fun getWebhook(slackChannel: SlackChannel) = when(slackChannel) {
        SlackChannel.ORUM -> orumSlackWebhook
    }
}