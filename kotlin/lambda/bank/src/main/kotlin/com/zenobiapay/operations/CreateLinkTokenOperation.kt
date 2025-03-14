package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.generated.models.CreateLinkToken200Response
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.operation.Operation
import com.zenobiapay.util.PlaidUtil
import javax.inject.Inject

class CreateLinkTokenOperation @Inject constructor(private val plaidUtil: PlaidUtil) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): CreateLinkToken200Response {
        val response = plaidUtil.createLinkToken(userId)
        context.logger.log("Got plaid response $response")

        return CreateLinkToken200Response(linkToken = response.linkToken)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
