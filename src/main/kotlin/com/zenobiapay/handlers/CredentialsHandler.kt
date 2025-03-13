package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.model.exception.UnknownPathException
import com.zenobiapay.operations.GenerateSecretsOperation
import com.zenobiapay.operations.GetAuthTokenOperation
import com.zenobiapay.util.ResponseHandler
import javax.inject.Inject

class CredentialsHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var generateSecretsOperation: GenerateSecretsOperation

    @Inject
    lateinit var getAuthTokenOperation: GetAuthTokenOperation

    override fun handleRequest(
        input: APIGatewayProxyRequestEvent,
        context: Context
    ): APIGatewayProxyResponseEvent? {
        return when (input.path) {
            "/generate-secrets" -> responseHandler.returnApiGwResponse(generateSecretsOperation, input, context)
            "/get-auth-token" -> {
                responseHandler.wrapOperation { getAuthTokenOperation.run(input, context, "") }
            }
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
    }
}