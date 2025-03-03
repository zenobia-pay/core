package com.zenobiapay.model.api.transfer

import com.fasterxml.jackson.databind.ObjectMapper

data class FulfillTransferRequest(
    val transferRequestId: String,
    val debtorId: String,
    val accountId: String,
) {
    companion object {
        fun from(request: String, objectMapper: ObjectMapper): FulfillTransferRequest {
            return objectMapper.readValue(request, FulfillTransferRequest::class.java)
        }
    }
}
