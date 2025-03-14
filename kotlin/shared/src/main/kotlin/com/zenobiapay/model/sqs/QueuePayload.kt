package com.zenobiapay.model.sqs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueuePayload(
    val records: List<QueueRecord>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class QueueRecord(
    val messageId: String,
    val receiptHandle: String,
    val body: PayoutMessage
)
