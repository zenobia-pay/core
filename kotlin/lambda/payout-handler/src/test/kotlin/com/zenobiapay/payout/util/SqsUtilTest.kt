package com.zenobiapay.payout.util

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.function.Consumer

class SqsUtilTest {

    private val sqsClient = mockk<SqsClient>(relaxed = true)

    @Test
    fun `sendMessage passes request args and message body to client`() {
        val sqsUtil = SqsUtil(sqsClient)
        val message = "test"
        val requestId = "requestId"
        val queueUrl = "queueUrl"
        sqsUtil.sendMessage(message, queueUrl, requestId)

        verify {
            sqsClient.sendMessage(
                match<Consumer<SendMessageRequest.Builder>> {
                    val request = SendMessageRequest.builder().applyMutation(it).build()
                    request.queueUrl() == queueUrl &&
                        request.messageBody() == message &&
                        request.messageAttributes()["requestId"]!!.stringValue() == requestId
                }
            )
        }
    }
}
