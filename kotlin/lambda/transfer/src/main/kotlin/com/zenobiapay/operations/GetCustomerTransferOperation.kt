package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.model.api.transfer.GetTransferRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.model.exception.ResourceNotFoundException
import com.zenobiapay.operation.Operation
import javax.inject.Inject

class GetCustomerTransferOperation @Inject constructor(
    private val transferDao: TransferDao,
    private val objectMapper: ObjectMapper
) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        val request = GetTransferRequest.from(input.queryStringParameters, objectMapper)
        val transfer = transferDao.getCustomerTransfer(userId, request.id) ?: throw ResourceNotFoundException("Transfer")
        return transfer
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}
