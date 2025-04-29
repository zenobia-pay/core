package com.zenobia.webhook.event.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.MetricHelper
import com.zenobia.webhook.event.di.DaggerAppComponent
import com.zenobia.webhook.event.logic.OrumWebhookLogic
import com.zenobia.webhook.event.logic.PlaidWebhookLogic
import com.zenobiapay.api.generated.model.OrumWebhookRequest
import com.zenobiapay.api.generated.model.PlaidWebhookRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.logging.log4j.ThreadContext
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class WebhookEventHandler: RequestHandler<SQSEvent, Unit> {

    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var orumWebhookLogic: OrumWebhookLogic

    @Inject
    lateinit var plaidWebhookLogic: PlaidWebhookLogic

    @Inject
    lateinit var metricHelper: MetricHelper

    override fun handleRequest(
        input: SQSEvent,
        context: Context?
    ) {
        try {
            ThreadContext.put("requestId", context!!.awsRequestId)
            input.records.forEach {
                handleRecord(it.body)
            }
        } finally {
            ThreadContext.clearAll()
        }
    }

    private fun handleRecord(recordBody: String) {
        val request = getWebhook(recordBody)

        when (request) {
            is OrumWebhookRequest -> {
                orumWebhookLogic.handleRequest(request)
                metricHelper.putMetric("OrumWebhookSuccess", 1.0)
            }
            is PlaidWebhookRequest -> {
                plaidWebhookLogic.handleRequest(request)
                metricHelper.putMetric("PlaidWebhookSuccess", 1.0)
            }
        }

    }

    private fun getWebhook(input: String): Any {
        return try {
            logger.info { "Attempting to get orum webhook" }
            getOrumWebhook(input)
        } catch (e: Exception) {
            logger.info { "Attempting to get plaid webhook" }
            getPlaidWebhook(input)
        }
    }

    private fun getOrumWebhook(input: String): OrumWebhookRequest {
        return objectMapper.readValue(input, OrumWebhookRequest::class.java)
    }

    private fun getPlaidWebhook(input: String): PlaidWebhookRequest {
        return objectMapper.readValue(input, PlaidWebhookRequest::class.java)
    }
}
