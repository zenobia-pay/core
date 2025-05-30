package com.zenobiapay.rds.model

import java.util.UUID

data class ItemMetadataSchema(
    val itemId: UUID = UUID.randomUUID(),
    val merchantId: String,
    val name: String,
    val productId: String?,
    val brandId: String?,
    val metadata: Map<String, Any>,
    val tags: List<String>
)
