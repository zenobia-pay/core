package com.zenobiapay.model.api.bank

import com.fasterxml.jackson.databind.ObjectMapper

data class ExchangeTokenRequest(val linkToken: String) {
    companion object {
        fun from(request: String, objectMapper: ObjectMapper): ExchangeTokenRequest {
            return objectMapper.readValue(request, ExchangeTokenRequest::class.java)
        }
    }
}
