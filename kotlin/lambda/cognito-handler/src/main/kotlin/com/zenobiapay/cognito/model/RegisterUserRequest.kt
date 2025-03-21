package com.zenobiapay.cognito.model

data class RegisterUserRequest(
    val sub: String,
    val firstName: String,
    val lastName: String,
    val email: String,
)
