package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import javax.inject.Inject

class DeleteM2MCredentialsOperation @Inject constructor(): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        TODO("Not yet implemented")
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        TODO("Not yet implemented")
    }
}