package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.generated.model.GetAdminTransfer200Response
import com.zenobiapay.api.generated.model.GetAdminTransferRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.util.getFee
import jakarta.inject.Inject

class GetAdminTransferOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao
): Operation<GetAdminTransferRequest, GetAdminTransfer200Response>() {
    override val inputType = GetAdminTransferRequest::class.java
    
    override fun run(
        request: GetAdminTransferRequest, 
        input: APIGatewayProxyRequestEvent, 
        context: Context, 
        userId: String?
    ): GetAdminTransfer200Response {
        val transferId = input.queryStringParameters?.get("id")
            ?: throw ResourceNotFoundException("Missing transfer ID")
            
        val transferItem = transferDao.getTransfer(transferId) ?: throw ResourceNotFoundException("TRANSFER")

        return GetAdminTransfer200Response()
            .amount(transferItem.amount)
            .transferRequestId(transferId)
            .inboundStatus(transferItem.inboundStatus.name)
            .outboundStatus(transferItem.outboundStatus.name)
            .statementItems(transferItem.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf())
            .statusMessage(transferItem.data?.statusMessage)
            .customerName(transferItem.data?.customer?.name)
            .fee(transferItem.data?.fee ?: transferItem.amount?.let { getFee(it) })
            .payoutTime(transferItem.data?.payoutTime)
            .creationTime(transferItem.data?.creationTime)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.ADMIN)
    }
}
