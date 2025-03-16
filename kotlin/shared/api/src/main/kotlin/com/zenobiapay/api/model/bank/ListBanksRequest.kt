package com.zenobiapay.api.model.bank

import com.fasterxml.jackson.databind.ObjectMapper

data class ListBanksRequest(
    val continuationToken: String? = null
) {
    companion object {
        fun from(request: Map<String, String>?, objectMapper: ObjectMapper): ListBanksRequest {
            if (request == null) {
                return ListBanksRequest()
            }
            return objectMapper.convertValue(request, ListBanksRequest::class.java)
        }
    }
}
