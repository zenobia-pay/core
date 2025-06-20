package com.zenobiapay.rds.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

data class ItemMetadata(
    val name: String,
    val brand: String?,
    val size: String?,
    val color: String?,
    val material: String?,
    val year: String?,
    val imageUrls: List<String>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RdsItemMetadataSchema(
    val itemId: UUID,
    val merchantId: String,
    val itemMetadata: ItemMetadata,
    val rawMetadata: Map<String, Any>?,
    val imageS3ObjectKeys: List<String>?,
)
