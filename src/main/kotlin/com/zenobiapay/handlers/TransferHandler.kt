package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.model.exception.UnknownPathException
import com.zenobiapay.operations.CreateTransferRequestOperation
import com.zenobiapay.operations.FulfillTransferOperation
import com.zenobiapay.operations.GetCustomerTransferOperation
import com.zenobiapay.operations.GetMerchantTransferOperation
import com.zenobiapay.operations.ListCustomerTransfersOperation
import com.zenobiapay.operations.ListMerchantTransfersOperation
import com.zenobiapay.util.ResponseHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var createTransferRequestOperation: CreateTransferRequestOperation

    @Inject
    lateinit var fulfillTransferOperation: FulfillTransferOperation

    @Inject
    lateinit var getCustomerTransferOperation: GetCustomerTransferOperation

    @Inject
    lateinit var getMerchantTransferOperation: GetMerchantTransferOperation

    @Inject
    lateinit var listCustomerTransfersOperation: ListCustomerTransfersOperation

    @Inject
    lateinit var listMerchantTransfersOperation: ListMerchantTransfersOperation

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        logger.info { "Got input $input" }
        val operation = when (input?.path) {
            "/create-transfer-request"  -> createTransferRequestOperation
            "/fulfill-transfer" -> fulfillTransferOperation
            "/get-customer-transfer" -> getCustomerTransferOperation
            "/get-merchant-transfer" -> getMerchantTransferOperation
            "/list-customer-transfers" -> listCustomerTransfersOperation
            "/list-merchant-transfers" -> listMerchantTransfersOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}
