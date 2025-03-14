package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrumCredentials(
    @JsonProperty("client_id")
    val clientId: String,
    @JsonProperty("client_secret")
    val clientSecret: String
)
