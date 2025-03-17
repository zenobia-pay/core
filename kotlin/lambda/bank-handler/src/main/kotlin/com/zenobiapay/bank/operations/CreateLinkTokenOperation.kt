package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.CreateLinkToken200Response
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.operation.Operation
import com.zenobiapay.plaid.PlaidWrapper
import javax.inject.Inject

class CreateLinkTokenOperation @Inject constructor(private val plaidWrapper: PlaidWrapper) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): CreateLinkToken200Response {
        val response = plaidWrapper.createLinkToken(userId)
        context.logger.log("Got plaid response $response")

        return CreateLinkToken200Response(linkToken = response.linkToken)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
