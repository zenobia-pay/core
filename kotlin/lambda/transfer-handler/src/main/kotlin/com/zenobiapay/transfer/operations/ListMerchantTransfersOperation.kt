package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.models.ListMerchantTransfers200Response
import com.zenobiapay.api.generated.models.ListMerchantTransfers200ResponseItemsInner
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.transfer.model.ListMerchantTransfersRequest
import javax.inject.Inject

class ListMerchantTransfersOperation @Inject constructor(private val objectMapper: ObjectMapper, private val transferDao: TransferDao) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): Any {
        val request = objectMapper.readValue(input.body, ListMerchantTransfersRequest::class.java)
        val (merchantTransfers, continuationToken) = transferDao.listMerchantTransfers(userId!!, request.continuationToken)
        return ListMerchantTransfers200Response(
            items = merchantTransfers.map {
                ListMerchantTransfers200ResponseItemsInner(
                    amount = it.amount,
                    status = it.status.name,
                    transferRequestId = it.requestId
                )
            },
            continuationToken = continuationToken?.encodeToken(objectMapper)
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
