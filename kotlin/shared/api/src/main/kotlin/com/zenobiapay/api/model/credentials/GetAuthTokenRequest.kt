package com.zenobiapay.api.model.credentials

data class GetAuthTokenRequest(
    val clientId: String,
    val clientSecret: String,
)
