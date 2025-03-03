package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.api.transfer.GetTransferRequest
import com.zenobiapay.model.api.transfer.GetTransferResponse
import com.zenobiapay.model.cognito.UserPoolGroup
import javax.inject.Inject

class GetTransferOperation @Inject constructor(private val objectMapper: ObjectMapper, private val transferDao: TransferDao): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): ApiResponse {
        val request = GetTransferRequest.from(input.queryStringParameters, objectMapper)

        val transferItem = transferDao.getTransferRequest(
            debtorId = userId,
            transferRequestId = request.id
        )
        return GetTransferResponse(
            transferRequestId = request.id,
            transferStatus = transferItem.status.toApiTransferStatus(),
            creditor = transferItem.data?.creditor?.toApiParticipantIdentity(),
            debtor = transferItem.data?.debtor?.toApiParticipantIdentity(),
            statementItems = transferItem.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf(),
            status = transferItem.status.toApiTransferStatus(),
            statusMessage = transferItem.data?.statusMessage
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}