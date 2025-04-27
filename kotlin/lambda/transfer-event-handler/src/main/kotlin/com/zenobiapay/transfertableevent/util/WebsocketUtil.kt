package com.zenobiapay.transfertableevent.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.TransferStatus
import com.zenobiapay.transfertableevent.di.TRANSFER_STATUS_NOTIFICATION_SECRET
import com.zenobiapay.transfertableevent.di.WEBSOCKET_SERVICE_ENDPOINT
import com.zenobiapay.transfertableevent.model.TransferWebsocketBody
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import jakarta.inject.Named

private val logger = KotlinLogging.logger {}

data class EncodedWebsocketPostBody(val token: String)

class WebsocketUtil @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Named(WEBSOCKET_SERVICE_ENDPOINT)
    private val websocketUrl: String,
    @Named(TRANSFER_STATUS_NOTIFICATION_SECRET)
    private val hmacSecret: String
) {
    fun sendWebsocketUpdate(transferRequestId: String, merchantId: String, status: TransferStatus) {
        logger.info { "Sending websocket update for request $transferRequestId, merchant $merchantId, status $status to $websocketUrl"}
        val websocketBody = TransferWebsocketBody(transferRequestId, merchantId, status)
        val encodedBody = EncodedWebsocketPostBody(websocketBody.generateSignedPayload(objectMapper, hmacSecret))
        val encodedBodyString = objectMapper.writeValueAsString(encodedBody)
        logger.info { "encoded body $encodedBody"}
        val request = Request.Builder()
            .url(websocketUrl)
            .post(encodedBodyString.toRequestBody())
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            logger.info { "Got response code ${response.code}" }
        } catch (e: IOException) {
            logger.error(e) { "Got exception when sending websocket to zenobia service. Swallowing." }
        }
    }
}