package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.FulfillTransferRequest
import com.zenobiapay.api.generated.model.GetCustomerTransfer200Response
import com.zenobiapay.api.generated.model.PaymentParticipantIdentity
import com.zenobiapay.api.model.NoApiBody
import com.zenobiapay.api.model.transfer.GetTransferRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.table.transfer.dao.TransferDao
import javax.inject.Inject

class GetCustomerTransferOperation @Inject constructor(
    private val transferDao: TransferDao,
    private val objectMapper: ObjectMapper
) : Operation<NoApiBody, GetCustomerTransfer200Response>() {

    override val inputType = NoApiBody::class.java

    override fun run(request: NoApiBody, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): GetCustomerTransfer200Response {
        val request = GetTransferRequest.from(input.queryStringParameters, objectMapper)
        val transfer = transferDao.getCustomerTransfer(userId!!, request.id) ?: throw ResourceNotFoundException("Transfer")
        return GetCustomerTransfer200Response()
            .transferRequestId(transfer.requestId)
            .merchant(PaymentParticipantIdentity().id(transfer.data?.merchant?.id).name(transfer.data?.merchant?.name))
            .status(transfer.status.toApiTransferStatus())
            .statementItems(transfer.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf())
            .statusMessage(transfer.data?.statusMessage)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}
