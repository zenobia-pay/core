package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.api.transfer.CreateTransferRequestRequest
import com.zenobiapay.model.api.transfer.CreateTransferRequestResponse
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.util.CognitoUtil
import javax.inject.Inject

class CreateTransferRequestOperation @Inject constructor(private val transferDao: TransferDao, private val objectMapper: ObjectMapper, private val cognitoUtil: CognitoUtil):
    Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): ApiResponse {
        val request = CreateTransferRequestRequest.from(input.body, objectMapper)
        val requestId = input.requestContext.requestId
        val userFullName = cognitoUtil.getUserFullName(userId)
        transferDao.putTransferRequest(userId, requestId, request.amount, userFullName, request.statementItems)

        return CreateTransferRequestResponse(transferRequestId = requestId, debtorId = userId)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}