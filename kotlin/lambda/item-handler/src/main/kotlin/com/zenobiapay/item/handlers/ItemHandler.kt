package com.zenobiapay.item.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.UnknownPathException
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.item.di.DaggerAppComponent
import com.zenobiapay.item.operations.CreateSellJobOperation
import com.zenobiapay.item.operations.GetItemOperation
import com.zenobiapay.item.operations.ListItemsOperation
import com.zenobiapay.item.operations.CompleteSellJobOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class ItemHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var getItemOperation: GetItemOperation
    
    @Inject
    lateinit var listItemsOperation: ListItemsOperation

    @Inject
    lateinit var completeSellJobOperation: CompleteSellJobOperation

    @Inject
    lateinit var createSellJobOperation: CreateSellJobOperation

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val operation = when (input.path) {
            "/get-item" -> getItemOperation
            "/list-items" -> listItemsOperation
            "/sell-item" -> completeSellJobOperation
            "/create-sell-job" -> createSellJobOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context)
    }
}