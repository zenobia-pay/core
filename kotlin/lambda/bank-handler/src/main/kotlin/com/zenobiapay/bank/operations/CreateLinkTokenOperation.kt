package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.model.Products
import com.zenobiapay.api.generated.model.CreateLinkToken200Response
import com.zenobiapay.api.generated.model.CreateLinkTokenRequest
import com.zenobiapay.api.model.NoApiBody
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.UnauthorizedException
import com.zenobiapay.api.util.getUserRole
import com.zenobiapay.plaid.PlaidWrapper
import javax.inject.Inject

class CreateLinkTokenOperation @Inject constructor(
    private val plaidWrapper: PlaidWrapper,
): Operation<CreateLinkTokenRequest, CreateLinkToken200Response>() {

    override val inputType = CreateLinkTokenRequest::class.java

    override fun run(request: CreateLinkTokenRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): CreateLinkToken200Response {
        if (input.requestContext.getUserRole() == UserPoolGroup.UNKNOWN &&
            request.product != CreateLinkTokenRequest.ProductEnum.IDENTITY_VERIFICATION) {
            throw UnauthorizedException()
        }

        val response = plaidWrapper.createLinkToken(userId!!, listOf(getPlaidProduct(request.product)))
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
        return listOf(UserPoolGroup.UNKNOWN, UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
