package com.zenobiapay.bank.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.bank.di.DaggerAppComponent
import com.zenobiapay.bank.operations.CreateLinkTokenOperation
import com.zenobiapay.api.model.exception.UnknownPathException
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.bank.operations.DeleteBankAccountOperation
import com.zenobiapay.bank.operations.ExchangeTokenOperation
import com.zenobiapay.bank.operations.ListBankAccountsOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class BankHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var createLinkTokenOperation: CreateLinkTokenOperation

    @Inject
    lateinit var exchangeTokenOperation: ExchangeTokenOperation

    @Inject
    lateinit var listBankAccountsOperation: ListBankAccountsOperation

    @Inject
    lateinit var deleteBankAccountOperation: DeleteBankAccountOperation

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        logger.info { "Got input $input" }
        val operation = when (input?.path) {
            "/create-link-token" -> createLinkTokenOperation
            "/exchange-token" -> exchangeTokenOperation
            "/list-bank-accounts" -> listBankAccountsOperation
            "/delete-bank-account" -> deleteBankAccountOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}