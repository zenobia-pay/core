package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.generated.models.CreateTransferRequest200Response
import com.zenobiapay.generated.models.CreateTransferRequestRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.model.ddb.transfer.StatementItem
import com.zenobiapay.model.exception.InvalidRequestException
import com.zenobiapay.util.CognitoUtil
import javax.inject.Inject

class CreateTransferRequestOperation @Inject constructor(private val transferDao: TransferDao, private val objectMapper: ObjectMapper, private val cognitoUtil: CognitoUtil):
    Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        val request = objectMapper.readValue<CreateTransferRequestRequest>(input.body)

        request.amount ?: throw InvalidRequestException("Amount not specified")

        val requestId = input.requestContext.requestId
        val userFullName = cognitoUtil.getUserFullName(userId)
        transferDao.putTransferRequest(
            userId,
            requestId,
            request.amount,
            userFullName,
            request.statementItems?.map {
                StatementItem.fromApiRequestStatementItem(it)
            } ?: listOf()
        )

        return CreateTransferRequest200Response(
            transferRequestId = requestId,
            merchantId = userId
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}