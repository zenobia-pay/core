package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zenobiapay.api.generated.models.CreateTransferRequest200Response
import com.zenobiapay.api.generated.models.CreateTransferRequestRequest
import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.cognito.CognitoUtil
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.StatementItem
import com.zenobiapay.table.user.dao.UserDao
import javax.inject.Inject

class CreateTransferRequestOperation @Inject constructor(
    private val transferDao: TransferDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper,
    private val cognitoUtil: CognitoUtil
): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): Any {
        val request = objectMapper.readValue<CreateTransferRequestRequest>(input.body)

        val requestId = input.requestContext.requestId
        val merchantName = userDao.getMerchant(userId!!)!!.data.displayName ?: cognitoUtil.getUserFullName(userId)
        transferDao.putTransferRequest(
            userId,
            requestId,
            request.amount,
            merchantName,
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
