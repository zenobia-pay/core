package com.zenobiapay.model.api.transfer

import com.fasterxml.jackson.databind.ObjectMapper

data class GetTransferRequest(
    val id: String,
) {
    companion object {
        fun from(request: Map<String, String>?, objectMapper: ObjectMapper): GetTransferRequest {
            return objectMapper.convertValue(request!!, GetTransferRequest::class.java)
        }
    }
}
