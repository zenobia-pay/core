package com.zenobiapay.transfertableevent.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.TransferStatus
import com.zenobiapay.transfertableevent.model.TransferWebhookBody
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.time.Instant
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class WebhookUtil @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val jwtUtil: JwtUtil,
) {
    companion object {
        const val EXPIRY_OFFSET_SECONDS = 60 * 5L // 5 minutes
    }

    fun sendTransferStatus(
        webhookUrl: String,
        userId: String,
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
        )
        val body = objectMapper.writeValueAsString(webhookBody)
        val mapBody = objectMapper.convertValue(webhookBody, object: TypeReference<Map<String, Any?>>() {})
        val signature = jwtUtil.signJwtWithKms(mapBody, userId)
        logger.info { "Sending webhook to url $webhookUrl with body $body" }
        val request = Request.Builder()
            .url(webhookUrl)
            .post(body.toRequestBody())
            .header("Authorization", "Bearer $signature")
            .build()

        try {
            okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            logger.error(e) { "Got exception when sending webhook $webhookBody. Swallowing." }
        }
    }
}