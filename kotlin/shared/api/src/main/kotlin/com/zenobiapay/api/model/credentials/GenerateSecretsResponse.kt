package com.zenobiapay.api.model.credentials

import com.zenobiapay.api.model.ApiResponse

data class GenerateSecretsResponse(
    val clientId: String,
    val clientSecret: String
): ApiResponse
