package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.generated.models.GetMerchantTransfer200Response
import com.zenobiapay.model.api.transfer.GetTransferRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import javax.inject.Inject

class GetMerchantTransferOperation @Inject constructor(private val objectMapper: ObjectMapper, private val transferDao: TransferDao) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        val request = GetTransferRequest.from(input.queryStringParameters, objectMapper)

        val transferItem = transferDao.getMerchantTransfer(
            merchantId = userId,
            transferRequestId = request.id
        )
        return GetMerchantTransfer200Response(
            transferRequestId = request.id,
            status = transferItem.status.toApiTransferStatus(),
            merchant = transferItem.data?.merchant?.toApiParticipantIdentity(),
            statementItems = transferItem.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf(),
            statusMessage = transferItem.data?.statusMessage
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
