package com.zenobiapay.util

fun getCorsHeaders() = mapOf(
    "Access-Control-Allow-Origin" to "https://www.zenobiapay.com",
    "Access-Control-Allow-Headers" to "Content-Type,Authorization",
    "Access-Control-Allow-Methods" to "GET, OPTIONS"
)
