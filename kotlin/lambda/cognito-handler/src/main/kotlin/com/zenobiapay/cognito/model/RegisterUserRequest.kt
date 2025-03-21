package com.zenobiapay.cognito.model

data class RegisterUserRequest(
    val sub: String,
    val userName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
)
