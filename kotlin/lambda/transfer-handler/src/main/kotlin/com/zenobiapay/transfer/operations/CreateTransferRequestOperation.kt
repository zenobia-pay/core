package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.CreateTransferRequest200Response
import com.zenobiapay.api.generated.model.CreateTransferRequestRequest
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.util.getSubForM2M
import com.zenobiapay.api.util.getUserRole
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.StatementItem
import com.zenobiapay.table.user.dao.UserDao
import javax.inject.Inject

class CreateTransferRequestOperation @Inject constructor(
    private val transferDao: TransferDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper,
): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, sub: String?): Any {
        val request = objectMapper.readValue(input.body, CreateTransferRequestRequest::class.java)
        val userId = when (input.requestContext.getUserRole()) {
            UserPoolGroup.MERCHANT -> sub!!
            UserPoolGroup.MERCHANT_M2M -> input.requestContext.getSubForM2M()
            UserPoolGroup.CUSTOMER, UserPoolGroup.UNKNOWN -> throw InvalidRequestException("Invalid role for transfer request")
        }

        val requestId = input.requestContext.requestId
        val merchantName = userDao.getUserItem(userId!!)?.data?.merchantData?.displayName
            ?: throw InvalidRequestException("Merchant display name not configured.")
        transferDao.putTransferRequest(
            userId,
            requestId,
            request.amount,
            merchantName,
            request.statementItems?.map {
                StatementItem.fromApiRequestStatementItem(it)
            } ?: listOf()
        )

        return CreateTransferRequest200Response()
            .transferRequestId(requestId)
            .merchantId(userId)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT, UserPoolGroup.MERCHANT_M2M)
    }
}
