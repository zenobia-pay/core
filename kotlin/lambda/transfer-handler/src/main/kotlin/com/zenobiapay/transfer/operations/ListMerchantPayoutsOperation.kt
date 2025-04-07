package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListMerchantPayouts200Response
import com.zenobiapay.api.generated.model.ListMerchantPayouts200ResponseItemsInner
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.util.getFee
import com.zenobiapay.transfer.model.ListMerchantTransfersRequest
import javax.inject.Inject

class ListMerchantPayoutsOperation @Inject constructor(private val transferDao: TransferDao, private val objectMapper: ObjectMapper): Operation<ListMerchantTransfersRequest, ListMerchantPayouts200Response>() {
    override val inputType = ListMerchantTransfersRequest::class.java
    override fun run(
        request: ListMerchantTransfersRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListMerchantPayouts200Response {
        userId!!
        val (payouts, continuationToken) = transferDao.listMerchantPayouts(userId, request.continuationToken)
        return ListMerchantPayouts200Response()
            .continuationToken(continuationToken?.encodeToken(objectMapper))
            .items(
                payouts.map {
                    val itemData = it.data
                    val (merchantAmount, fee) = if (itemData != null) {
                        it.data!!.merchantAmount to it.data!!.feeAmount
                    } else {
                        val fee = getFee(it.amount)
                        (it.amount - fee) to fee
                    }
                    val status = if (itemData?.merchantPaid == true) {
                        ListMerchantPayouts200ResponseItemsInner.StatusEnum.PAID
                    } else {
                        ListMerchantPayouts200ResponseItemsInner.StatusEnum.PENDING
                    }
                    ListMerchantPayouts200ResponseItemsInner()
                        .status(status)
                        .amount(merchantAmount)
                        .fee(fee)
                }
            )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}