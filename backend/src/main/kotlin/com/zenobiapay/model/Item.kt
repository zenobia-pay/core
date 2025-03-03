package com.zenobiapay.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String,
    val name: String,
    val price: Double,
    val description: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ItemResponse(
    val items: List<Item>
)

@Serializable
data class ErrorResponse(
    val error: String
)