package com.zenobiapay.transfertableevent.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.TransferStatus
import com.zenobiapay.table.util.signHmacSha256
import java.util.Base64

/**
 * Gets sent to web socket service for frontend use.
 */
data class TransferWebsocketBody(
    val transferRequestId: String,
    val merchantId: String,
    val status: TransferStatus
) {
    fun generateSignedPayload(objectMapper: ObjectMapper, hmacSecret: String): String {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(this))
        val signature = signHmacSha256(payload, hmacSecret)
        return "$payload.$signature"
    }
}
