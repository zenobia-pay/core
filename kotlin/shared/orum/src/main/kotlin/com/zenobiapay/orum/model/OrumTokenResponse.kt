package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrumTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Int
)
