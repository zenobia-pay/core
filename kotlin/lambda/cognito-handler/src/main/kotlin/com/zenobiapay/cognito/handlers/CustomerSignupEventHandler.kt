package com.zenobiapay.cognito.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.cognito.di.DaggerAppComponent
import com.zenobiapay.cognito.operations.RegisterUserOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class CustomerSignupEventHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var registerUserOperation: RegisterUserOperation

    override fun handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        logger.info { "Got event $event" }

        val operation = when (event?.path) {
            "/register-user" -> registerUserOperation
            else -> throw UnsupportedOperationException()
        }
        return responseHandler.returnApiGwResponse(operation, event, context)
    }
}