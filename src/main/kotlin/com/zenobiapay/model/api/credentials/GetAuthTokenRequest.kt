package com.zenobiapay.model.api.credentials

data class GetAuthTokenRequest(
    val clientId: String,
    val clientSecret: String,
)
