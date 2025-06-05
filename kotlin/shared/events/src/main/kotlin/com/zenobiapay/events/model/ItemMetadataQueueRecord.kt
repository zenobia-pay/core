package com.zenobiapay.events.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

enum class ItemMetadataQueueRecordType(value: String) {
    PUT("PUT"),
    UPDATE("UPDATE");
}

@JsonIgnoreProperties(ignoreUnknown = true)
open class ItemMetadataQueueRecord() {
    open val type: ItemMetadataQueueRecordType? = null
}

data class PutItemMetadataQueueRecord(
    val merchantId: String,
    val transferMetadata: Map<String, Any>?,
    val transferRequestId: String,
    val creationTime: Instant,
    val itemMetadata: Map<String, Map<String, Any>>?,
): ItemMetadataQueueRecord() {
    override val type: ItemMetadataQueueRecordType = ItemMetadataQueueRecordType.PUT
}

data class UpdateItemMetadataQueueRecord(
    val transferRequestId: String,
    val ownershipTime: Instant,
    val ownerId: String,
): ItemMetadataQueueRecord() {
    override val type: ItemMetadataQueueRecordType = ItemMetadataQueueRecordType.UPDATE
}
