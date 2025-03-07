package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.model.exception.UnknownPathException
import com.zenobiapay.operations.*
import com.zenobiapay.util.ResponseHandler
import javax.inject.Inject

class TransferHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var createTransferRequestOperation: CreateTransferRequestOperation

    @Inject
    lateinit var fulfillTransferOperation: FulfillTransferOperation

    @Inject
    lateinit var getTransferOperation: GetTransferOperation

    @Inject
    lateinit var listTransfersOperation: ListTransfersOperation

    @Inject
    lateinit var listMerchantTransfersOperation: ListMerchantTransfersOperation

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        val operation = when (input?.path) {
            "/create-transfer-request" -> createTransferRequestOperation
            "/fulfill-transfer" -> fulfillTransferOperation
            "/get-transfer" -> getTransferOperation
            "/list-transfers" -> listTransfersOperation
            "/list-merchant-transfers" -> listMerchantTransfersOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}