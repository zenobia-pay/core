package com.zenobiapay.payout.model

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventBridgeEvent(
    var version: String? = null,
    var id: String? = null,
    var source: String? = null,
    var detail: DdbStreamDetail? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DdbStreamDetail(
    var eventName: String? = null,
    var dynamodb: DynamoRecord? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DynamoRecord(
    @JsonProperty("ApproximateCreationDateTime")
    var approximateCreationTime: Double? = null,
    @JsonProperty("StreamViewType")
    var streamViewType: String? = null,
    @JsonProperty("Keys")
    var keys: Map<String, Any>? = null,
    @JsonProperty("NewImage")
    var newImage: Map<String, Any>? = null,
    @JsonProperty("OldImage")
    var oldImage: Map<String, Any>? = null,
)
