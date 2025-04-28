package com.zenobia.webhook.event.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.MetricHelper
import com.zenobia.webhook.event.di.DaggerAppComponent
import com.zenobia.webhook.event.logic.OrumWebhookLogic
import com.zenobiapay.api.generated.model.OrumWebhookRequest
import org.apache.logging.log4j.ThreadContext
import javax.inject.Inject

class OrumWebhookEventHandler: RequestHandler<SQSEvent, Unit> {

    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var objectMapper: ObjectMapper
    @Inject
    lateinit var orumWebhookLogic: OrumWebhookLogic
    @Inject
    lateinit var metricHelper: MetricHelper

    override fun handleRequest(
        input: SQSEvent,
        context: Context?
    ) {
        try {
            ThreadContext.put("requestId", context!!.awsRequestId)
            input.records.forEach {
                val request = getWebhook(it.body)
                orumWebhookLogic.handleRequest(request)
                metricHelper.putMetric("Success", 1.0)
            }
        } finally {
            ThreadContext.clearAll()
        }
    }

    private fun getWebhook(input: String): OrumWebhookRequest {
        return objectMapper.readValue(input, OrumWebhookRequest::class.java)
    }

}
