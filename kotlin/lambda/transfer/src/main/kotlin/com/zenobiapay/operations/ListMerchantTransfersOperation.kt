package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.generated.models.ListMerchantTransfers200Response
import com.zenobiapay.generated.models.ListMerchantTransfers200ResponseItemsInner
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.operation.Operation
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
