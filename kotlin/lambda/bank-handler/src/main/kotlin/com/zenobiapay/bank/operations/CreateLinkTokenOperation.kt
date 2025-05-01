package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.plaid.client.model.Products
import com.zenobiapay.api.generated.model.CreateLinkToken200Response
import com.zenobiapay.api.generated.model.CreateLinkTokenRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.bank.di.API_GATEWAY_ENDPOINT
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.table.user.dao.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import jakarta.inject.Inject
import jakarta.inject.Named

private val logger = KotlinLogging.logger {}

class CreateLinkTokenOperation @Inject constructor(
    private val plaidWrapper: PlaidWrapper,
    private val userDao: UserDao,
    @Named(API_GATEWAY_ENDPOINT) private val apiGatewayEndpoint: String,
): Operation<CreateLinkTokenRequest, CreateLinkToken200Response>() {

    override val inputType = CreateLinkTokenRequest::class.java

    override fun run(request: CreateLinkTokenRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): CreateLinkToken200Response {
        val sub = userId
            ?: UUID.randomUUID().toString().also {
                logger.info { "Generated new random sub $it" }
                userDao.createTemporaryCustomer(it)
                logger.info { "Successfully wrote generated sub to user table" }
            }
        val plaidWebhook = apiGatewayEndpoint + "plaid-webhook"
        logger.info { "Using plaid webhook $plaidWebhook" }
        val response = plaidWrapper.createLinkToken(sub, plaidWebhook, getPlaidProducts(request.product))
        return CreateLinkToken200Response()
            .linkToken(response.linkToken)
            .sub(sub)
    }

    private fun getPlaidProducts(product: CreateLinkTokenRequest.ProductEnum): List<Products> {
        return when (product) {
            // TODO: re-enable plaid signal
//            CreateLinkTokenRequest.ProductEnum.AUTH -> listOf(Products.AUTH, Products.IDENTITY, Products.SIGNAL)
            CreateLinkTokenRequest.ProductEnum.AUTH -> listOf(Products.AUTH, Products.IDENTITY)
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN, UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
