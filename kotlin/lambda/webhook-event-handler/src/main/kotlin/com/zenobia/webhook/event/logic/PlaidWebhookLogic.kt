package com.zenobia.webhook.event.logic

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.webhook.event.util.SlackChannel
import com.zenobia.webhook.event.util.SlackUtil
import com.zenobiapay.api.generated.model.PlaidWebhookRequest
import javax.inject.Inject

class PlaidWebhookLogic @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val slackUtil: SlackUtil,
) {
    fun handleRequest(request: PlaidWebhookRequest) {
        slackUtil.sendMessage(
            """
                ${request.webhookCode} - ${request.webhookType}
                - Error: `${request.error.errorCode}`: ${request.error.errorMessage}
                - Item id: `${request.itemId}`
                - Environment: `${request.environment}`
            """.trimIndent(),
            SlackChannel.PLAID
        )
    }
}