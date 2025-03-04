package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.api.transfer.ListTransferItem
import com.zenobiapay.model.api.transfer.ListTransfersResponse
import com.zenobiapay.model.cognito.UserPoolGroup
import javax.inject.Inject

class ListTransfersOperation @Inject constructor(private val transferDao: TransferDao): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): ApiResponse {
        val transfers = transferDao.listCustomerTransfers(userId)
        return ListTransfersResponse(
            transfers.map { ListTransferItem.fromTransferRequestItem(it) }
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}