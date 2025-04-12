package com.zenobiapay.transfer.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.table.util.signHmacSha256
import java.util.Base64

/**
 * Returned encoded in base64 during create transfer request. Our frontend library uses this to subscribe to webhooks
 */
data class TransferRequestWebsocketSignature(
    val transferRequestId: String,
    val merchantId: String,
    val expiry: Long,
) {
    fun toSignedHmacString(objectMapper: ObjectMapper, hmacSecret: String): String {
        val encodedBody = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(this))
        val signature = signHmacSha256(encodedBody, hmacSecret)
        return "$encodedBody.$signature"
    }
}
