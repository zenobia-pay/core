package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.transfer.GetTransferRequest
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.exception.ResourceNotFoundException
import com.zenobiapay.table.transfer.dao.TransferDao
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
