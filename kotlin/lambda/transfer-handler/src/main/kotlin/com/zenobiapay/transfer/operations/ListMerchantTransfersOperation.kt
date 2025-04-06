package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListMerchantTransfers200Response
import com.zenobiapay.api.generated.model.ListMerchantTransfers200ResponseItemsInner
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.transfer.model.ListMerchantTransfersRequest
import javax.inject.Inject

class ListMerchantTransfersOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao
) : Operation<ListMerchantTransfersRequest, ListMerchantTransfers200Response>() {

    override val inputType = ListMerchantTransfersRequest::class.java

    override fun run(
        request: ListMerchantTransfersRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListMerchantTransfers200Response {
        val (merchantTransfers, continuationToken) = transferDao.listMerchantTransfers(userId!!, request.continuationToken)
        return ListMerchantTransfers200Response()
            .items(merchantTransfers.map {
                ListMerchantTransfers200ResponseItemsInner()
                    .amount(it.amount)
                    .status(it.status.name)
                    .transferRequestId(it.requestId)
            })
            .continuationToken(continuationToken?.encodeToken(objectMapper))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
