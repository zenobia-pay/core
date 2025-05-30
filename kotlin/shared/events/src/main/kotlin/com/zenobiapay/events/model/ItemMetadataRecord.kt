package com.zenobiapay.events.model

data class ItemMetadataRecord(
    val merchantId: String,
    val transferMetadata: Map<String, Any>?,
    val transferRequestId: String,
    val itemMetadata: Map<String, Map<String, Any>>?,
)
