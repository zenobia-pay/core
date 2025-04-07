package com.zenobiapay.webhook.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.zenobiapay.api.model.exception.UnknownPathException
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.webhook.di.DaggerAppComponent
import com.zenobiapay.webhook.operation.OrumWebhookOperation
import jakarta.inject.Inject

class WebhookHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var orumWebhookOperation: OrumWebhookOperation

    @Inject
    lateinit var responseHandler: ResponseHandler

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(
        input: APIGatewayProxyRequestEvent?,
        context: Context?
    ): APIGatewayProxyResponseEvent? {
        val operation = when (input?.path) {
            "/orum-webhook" -> orumWebhookOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}