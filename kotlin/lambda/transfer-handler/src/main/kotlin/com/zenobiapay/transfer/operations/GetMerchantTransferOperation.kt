package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.model.transfer.GetMerchantTransferRequest
import com.zenobiapay.api.generated.model.GetMerchantTransfer200Response
import com.zenobiapay.api.model.NoApiBody
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.util.getFee
import jakarta.inject.Inject

class GetMerchantTransferOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao
): Operation<NoApiBody, GetMerchantTransfer200Response>() {
    override val inputType = NoApiBody::class.java
    override fun run(request: NoApiBody, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): GetMerchantTransfer200Response {
        val request = GetMerchantTransferRequest.from(input.queryStringParameters, objectMapper)

        val transferItem = transferDao.getTransfer(
            transferRequestId = request.id
        )
        if (transferItem == null || transferItem.data?.merchant?.id != userId) {
            throw ResourceNotFoundException("TRANSFER")
        }
        return GetMerchantTransfer200Response()
            .amount(transferItem.amount)
            .transferRequestId(request.id)
            .status(transferItem.outboundStatus.toApiTransferStatus())
            .statementItems(transferItem.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf())
            .statusMessage(transferItem.data?.statusMessage)
            .customerName(transferItem.data?.customer?.name)
            .fee(transferItem.data?.fee ?: transferItem.amount?.let { getFee(it) })
            .payoutTime(transferItem.data?.payoutTime)
            .creationTime(transferItem.data?.creationTime)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
