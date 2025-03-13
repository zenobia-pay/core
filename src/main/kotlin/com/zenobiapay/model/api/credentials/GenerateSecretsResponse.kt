package com.zenobiapay.model.api.credentials

import com.zenobiapay.model.api.ApiResponse

data class GenerateSecretsResponse(
    val clientId: String,
    val clientSecret: String
): ApiResponse
