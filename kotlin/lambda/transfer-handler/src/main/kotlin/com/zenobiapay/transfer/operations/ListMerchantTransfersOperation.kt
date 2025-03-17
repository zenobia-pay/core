package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.ListMerchantTransfers200Response
import com.zenobiapay.api.generated.models.ListMerchantTransfers200ResponseItemsInner
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.transfer.dao.TransferDao
import javax.inject.Inject

class ListMerchantTransfersOperation @Inject constructor(private val transferDao: TransferDao) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        val merchantTransfers = transferDao.listMerchantTransfers(userId)
        return ListMerchantTransfers200Response(
            items = merchantTransfers.map {
                ListMerchantTransfers200ResponseItemsInner(
                    amount = it.amount,
                    status = it.status.name,
                    transferRequestId = it.requestId
                )
            }
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
