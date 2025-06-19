package com.zenobiapay.rds.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class ItemMetadataSchema(
    val itemId: UUID = UUID.randomUUID(),
    val merchantId: String?,
    val name: String,
    val brandName: String?,
    val size: String?,
    val color: String?,
    val material: String?,
    val year: String?,
    val metadata: Map<String, Any>?,
    val imageUrls: List<String>?,
)
