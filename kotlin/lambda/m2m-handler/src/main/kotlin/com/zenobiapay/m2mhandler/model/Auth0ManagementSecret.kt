package com.zenobiapay.m2mhandler.model

data class Auth0ManagementSecret(
    val clientId: String,
    val clientSecret: String,
    val domain: String,
    val audience: String,
)
