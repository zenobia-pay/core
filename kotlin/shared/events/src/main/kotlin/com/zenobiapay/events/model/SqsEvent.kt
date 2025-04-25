package com.zenobiapay.events.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SqsEvent (
    @JsonProperty("Records")
    val records: List<SqsRecord>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SqsRecord(
    val messageId: String,
    val body: String,
)