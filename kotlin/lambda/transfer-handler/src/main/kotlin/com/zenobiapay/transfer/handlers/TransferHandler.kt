package com.zenobiapay.transfer.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.UnknownPathException
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.transfer.di.DaggerAppComponent
import com.zenobiapay.transfer.operations.CreateTransferRequestOperation
import com.zenobiapay.transfer.operations.FulfillTransferOperation
import com.zenobiapay.transfer.operations.GetCustomerTransferOperation
import com.zenobiapay.transfer.operations.GetMerchantTransferOperation
import com.zenobiapay.transfer.operations.ListCustomerTransfersOperation
import com.zenobiapay.transfer.operations.ListMerchantPayoutsOperation
import com.zenobiapay.transfer.operations.ListMerchantTransfersOperation
import com.zenobiapay.transfer.operations.MarkInDisputeOperation
import com.zenobiapay.transfer.operations.EditTransferOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

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

    @Inject
    lateinit var listMerchantPayoutsOperation: ListMerchantPayoutsOperation

    @Inject
    lateinit var editTransferOperation: EditTransferOperation

    @Inject
    lateinit var markInDisputeOperation: MarkInDisputeOperation

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
            "/list-merchant-payouts" -> listMerchantPayoutsOperation
            "/edit-transfer" -> editTransferOperation
            "/mark-in-dispute" -> markInDisputeOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}