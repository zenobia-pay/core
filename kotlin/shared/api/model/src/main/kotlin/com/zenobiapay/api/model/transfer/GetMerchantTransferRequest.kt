package com.zenobiapay.api.model.transfer

import com.fasterxml.jackson.databind.ObjectMapper

data class GetMerchantTransferRequest(
    val id: String
) {
    companion object {
        fun from(request: Map<String, String>?, objectMapper: ObjectMapper): GetMerchantTransferRequest {
            return objectMapper.convertValue(request!!, GetMerchantTransferRequest::class.java)
        }
    }
}
