package com.zenobiapay.transfertableevent.util

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import jakarta.inject.Inject

class SqsUtil @Inject constructor(private val sqsClient: SqsClient) {
    companion object {
        const val REQUEST_ID_KEY = "requestId"
    }

    fun sendMessage(message: String, queueUrl: String, requestId: String) {
        sqsClient.sendMessage {
            it.queueUrl(queueUrl)
                .messageBody(message)
                .messageAttributes(
                    mapOf(REQUEST_ID_KEY to generateMessageAttributeValue(requestId))
                )
        }
    }

    private fun generateMessageAttributeValue(s: String) =
        MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(s)
            .build()
}
