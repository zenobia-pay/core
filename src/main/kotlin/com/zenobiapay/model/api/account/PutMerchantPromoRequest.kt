package com.zenobiapay.model.api.account

import com.fasterxml.jackson.databind.ObjectMapper

data class PutMerchantPromoRequest(
    val goalTarget: Int,
    val creditAmount: Int,
) {
    companion object {
        fun from(json: String, objectMapper: ObjectMapper): PutMerchantPromoRequest =
            objectMapper.readValue(json, PutMerchantPromoRequest::class.java)
    }
}
