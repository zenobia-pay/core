package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.generated.models.ListCustomerTransfers200Response
import com.zenobiapay.generated.models.ListCustomerTransfers200ResponseItemsInner
import com.zenobiapay.model.cognito.UserPoolGroup
import javax.inject.Inject

class ListCustomerTransfersOperation @Inject constructor(private val transferDao: TransferDao): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): ListCustomerTransfers200Response {
        val transfers = transferDao.listCustomerTransfers(userId)
        return ListCustomerTransfers200Response(
            items = transfers.map {
                ListCustomerTransfers200ResponseItemsInner(
                    amount = it.amount,
                    status = it.status.toApiTransferStatus(),
                    merchant = it.data!!.merchant!!.toApiParticipantIdentity(),
                    creationTime = it.data!!.creationTime
                )
            }
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}