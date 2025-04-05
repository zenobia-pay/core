package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.model.Products
import com.zenobiapay.api.generated.model.CreateLinkToken200Response
import com.zenobiapay.api.generated.model.CreateLinkTokenRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.plaid.PlaidWrapper
import javax.inject.Inject

class CreateLinkTokenOperation @Inject constructor(private val objectMapper: ObjectMapper, private val plaidWrapper: PlaidWrapper) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): CreateLinkToken200Response {
        val body = objectMapper.readValue(input.body, CreateLinkTokenRequest::class.java)
        val response = plaidWrapper.createLinkToken(userId!!, listOf(getPlaidProduct(body.product)))
        context.logger.log("Got plaid response $response")

        return CreateLinkToken200Response().linkToken(response.linkToken)
    }

    private fun getPlaidProduct(product: CreateLinkTokenRequest.ProductEnum): Products {
        return when (product) {
            CreateLinkTokenRequest.ProductEnum.AUTH -> Products.AUTH
            CreateLinkTokenRequest.ProductEnum.IDENTITY_VERIFICATION -> Products.IDENTITY_VERIFICATION
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
