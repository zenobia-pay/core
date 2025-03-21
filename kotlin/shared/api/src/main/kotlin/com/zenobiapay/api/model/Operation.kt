package com.zenobiapay.api.model

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.exception.UnauthorizedException
import com.zenobiapay.api.model.cognito.UserPoolGroup

abstract class Operation {
    abstract fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): Any

    abstract fun getUserPoolAllowList(): List<UserPoolGroup>

    fun assertUserPoolGroupValid(requestUserPoolGroups: List<UserPoolGroup>) {
        val isValid = requestUserPoolGroups.any {
            it in this.getUserPoolAllowList()
        }
        if (!isValid) {
            throw UnauthorizedException()
        }
    }
}