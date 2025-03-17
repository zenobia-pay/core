package com.zenobiapay.plaid.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PlaidCredentials(
    @JsonProperty("client_id")
    val clientId: String,
    @JsonProperty("client_secret")
    val secret: String,
    @JsonProperty("endpoint")
    val endpoint: String
) {
    fun toMap(): Map<String, String> = mapOf(
        "clientId" to clientId,
        "secret" to secret
    )
}
