package com.zenobiapay.bank.operations

import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.DeleteRefreshTokenRequest
import com.zenobiapay.api.generated.model.ExchangeTokenRequest
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.table.credentials.dao.CredentialsDao
import jakarta.inject.Inject

class DeleteRefreshTokenOperation @Inject constructor(
    private val credentialsDao: CredentialsDao
): Operation<DeleteRefreshTokenRequest, Unit>() {
    override val inputType = DeleteRefreshTokenRequest::class.java

    override fun run(request: DeleteRefreshTokenRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?) {
        userId!!
        credentialsDao.deleteRefreshToken(userId, request.refreshToken)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}
