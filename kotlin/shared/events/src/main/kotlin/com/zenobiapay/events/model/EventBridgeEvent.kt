package com.zenobiapay.events.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventBridgeEvent(
    val version: String,
    val id: String,
    val source: String,
    val detail: DdbStreamDetail,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DdbStreamDetail(
    val eventName: String,
    val dynamodb: DynamoRecord,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DynamoRecord(
    @JsonProperty("ApproximateCreationDateTime")
    val approximateCreationTime: Double? = null,
    @JsonProperty("StreamViewType")
    val streamViewType: String? = null,
    @JsonProperty("Keys")
    val keys: Map<String, Any>? = null,
    @JsonProperty("NewImage")
    val newImage: Map<String, Any>? = null,
    @JsonProperty("OldImage")
    val oldImage: Map<String, Any>? = null,
)
