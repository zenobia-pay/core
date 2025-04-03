package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.models.ListCustomerTransfers200Response
import com.zenobiapay.api.generated.models.ListCustomerTransfers200ResponseItemsInner
import com.zenobiapay.api.generated.models.ListCustomerTransfersRequest
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.transfer.dao.TransferDao
import javax.inject.Inject

class ListCustomerTransfersOperation @Inject constructor(private val objectMapper: ObjectMapper, private val transferDao: TransferDao) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ListCustomerTransfers200Response {
        val request = objectMapper.readValue(input.body, ListCustomerTransfersRequest::class.java)
        val (transfers, continuationToken) = transferDao.listCustomerTransfers(userId!!, request.continuationToken)
        return ListCustomerTransfers200Response(
            items = transfers.map {
                ListCustomerTransfers200ResponseItemsInner(
                    amount = it.amount,
                    status = it.status.toApiTransferStatus(),
                    merchant = it.data!!.merchant!!.toApiParticipantIdentity(),
                    creationTime = it.data!!.creationTime
                )
            },
            continuationToken = continuationToken?.encodeToken(objectMapper)
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}
