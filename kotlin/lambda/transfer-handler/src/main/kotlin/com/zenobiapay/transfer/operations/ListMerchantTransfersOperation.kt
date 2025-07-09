package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListMerchantTransfers200Response
import com.zenobiapay.api.generated.model.ListMerchantTransfers200ResponseItemsInner
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.table.model.BadTokenException
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.transfer.di.PAGINATION_SECRET
import com.zenobiapay.transfer.model.ListMerchantTransfersRequest
import jakarta.inject.Inject
import jakarta.inject.Named

class ListMerchantTransfersOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao,
    @Named(PAGINATION_SECRET) private val paginationSecret: String
) : Operation<ListMerchantTransfersRequest, ListMerchantTransfers200Response>() {

    override val inputType = ListMerchantTransfersRequest::class.java

    override fun run(
        request: ListMerchantTransfersRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListMerchantTransfers200Response {
        val (merchantTransfers, continuationToken) = try {
            transferDao.listMerchantTransfers(userId!!, request.continuationToken, paginationSecret)
        } catch (e: BadTokenException) {
            throw InvalidRequestException("Bad token")
        }
        return ListMerchantTransfers200Response()
            .items(merchantTransfers.map {
                ListMerchantTransfers200ResponseItemsInner()
                    .amount(it.amount)
                    .status(it.inboundStatus.toApiTransferStatus().name)
                    .transferRequestId(it.requestId)
                    .customerName(it.data?.customer?.name)
                    .fee(it.data?.fee)
                    .payoutTime(it.data?.payoutTime)
                    .creationTime(it.data?.creationTime)
            })
            .continuationToken(continuationToken?.encodeToken(objectMapper, paginationSecret))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
