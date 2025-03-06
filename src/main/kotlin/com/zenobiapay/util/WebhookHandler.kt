package com.zenobiapay.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.model.api.transfer.TransferStatus
import com.zenobiapay.model.webhook.TransferWebhookBody
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.time.Instant
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class WebhookHandler @Inject constructor(private val okHttpClient: OkHttpClient, private val objectMapper: ObjectMapper) {
    companion object {
        const val EXPIRY_OFFSET_SECONDS = 60 * 5L // 5 minutes
    }

    fun sendTransferStatus(
        webhookUrl: String,
        transferRequestId: String,
        status: TransferStatus,
        amount: Int,
        timestamp: Instant = Instant.now()
    ) {
        val expiryTime = timestamp.plusSeconds(EXPIRY_OFFSET_SECONDS)
        val webhookBody = TransferWebhookBody(
            transferRequestId = transferRequestId,
            amount = amount,
            status = status,
            expiry = expiryTime.toString()
        )
        val body = objectMapper.writeValueAsString(webhookBody)
        logger.info { "Sending webhook to url $webhookUrl with body $body" }
        val request = Request.Builder() // TODO: add authentication headers
            .url(webhookUrl)
            .post(body.toRequestBody())
            .build()

        try {
            okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            logger.error(e) { "Got exception when sending webhook $webhookBody. Swallowing." }
        }
    }
}