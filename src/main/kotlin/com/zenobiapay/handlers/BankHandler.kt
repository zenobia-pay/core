package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.model.exception.UnknownPathException
import com.zenobiapay.operations.CreateLinkTokenOperation
import com.zenobiapay.operations.ExchangeTokenOperation
import com.zenobiapay.operations.ListBanksOperation
import com.zenobiapay.util.ResponseHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class BankHandler: RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var createLinkTokenOperation: CreateLinkTokenOperation

    @Inject
    lateinit var exchangeTokenOperation: ExchangeTokenOperation

    @Inject
    lateinit var listBanksOperation: ListBanksOperation

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        val operation = when (input?.path) {
            "/create-link-token" -> createLinkTokenOperation
            "/exchange-token" -> exchangeTokenOperation
            "/list-banks" -> listBanksOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}