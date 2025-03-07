package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.model.exception.UnknownPathException
import com.zenobiapay.operations.GetMerchantOperation
import com.zenobiapay.operations.UpdateMerchantOperation
import com.zenobiapay.util.ResponseHandler
import javax.inject.Inject

class UserHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var updateMerchantOperation: UpdateMerchantOperation

    @Inject
    lateinit var getMerchantOperation: GetMerchantOperation

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        val operation = when (input?.path) {
            "/update-merchant" -> updateMerchantOperation
            "/get-merchant" -> getMerchantOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}