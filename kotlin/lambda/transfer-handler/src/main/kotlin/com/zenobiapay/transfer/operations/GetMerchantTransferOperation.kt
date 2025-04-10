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
import javax.inject.Inject

class GetMerchantTransferOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao
): Operation<NoApiBody, GetMerchantTransfer200Response>() {
    override val inputType = NoApiBody::class.java
    override fun run(request: NoApiBody, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): GetMerchantTransfer200Response {
        val request = GetMerchantTransferRequest.from(input.queryStringParameters, objectMapper)

        val transferItem = transferDao.getMerchantTransfer(
            merchantId = userId!!,
            transferRequestId = request.id
        ) ?: throw ResourceNotFoundException("TRANSFER")
        return GetMerchantTransfer200Response()
            .amount(transferItem.amount)
            .transferRequestId(request.id)
            .status(transferItem.status.toApiTransferStatus())
            .statementItems(transferItem.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf())
            .statusMessage(transferItem.data?.statusMessage)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
