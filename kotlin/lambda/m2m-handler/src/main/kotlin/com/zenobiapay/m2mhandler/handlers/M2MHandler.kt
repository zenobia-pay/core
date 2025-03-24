package com.zenobiapay.m2mhandler.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.zenobiapay.api.exception.UnknownPathException
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.m2mhandler.di.DaggerAppComponent
import com.zenobiapay.m2mhandler.operations.CreateM2MCredentialsOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class M2MHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var createM2MCredentialsOperation: CreateM2MCredentialsOperation

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        logger.info { "Got input $input" }
        val operation = when (input?.path) {
            "/create-m2m-credentials"  -> createM2MCredentialsOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}
