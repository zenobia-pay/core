package com.zenobiapay.api.operation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.model.cognito.UserPoolGroup

abstract class Operation<I, O> {
    abstract val inputType: Class<I>
    abstract fun run(request: I, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): O

    abstract fun getUserPoolAllowList(): List<UserPoolGroup>
}