package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListAdminTransfersRequest
import com.zenobiapay.api.generated.model.ListAdminTransfers200Response
import com.zenobiapay.api.generated.model.ListAdminTransfers200ResponseItemsInner
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.table.model.BadTokenException
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.util.getFee
import com.zenobiapay.transfer.di.PAGINATION_SECRET
import jakarta.inject.Inject
import jakarta.inject.Named

class ListAdminTransfersOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao,
    @Named(PAGINATION_SECRET) private val paginationSecret: String
) : Operation<ListAdminTransfersRequest, ListAdminTransfers200Response>() {

    override val inputType = ListAdminTransfersRequest::class.java

    override fun run(
        request: ListAdminTransfersRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListAdminTransfers200Response {
        val merchantId = request.sub
        val (merchantTransfers, continuationToken) = try {
            transferDao.listMerchantTransfers(merchantId, request.continuationToken, paginationSecret)
        } catch (e: BadTokenException) {
            throw InvalidRequestException("Bad token")
        }
        return ListAdminTransfers200Response()
            .items(merchantTransfers.map {
                ListAdminTransfers200ResponseItemsInner()
                    .amount(it.amount)
                    .status(it.inboundStatus.toApiTransferStatus().name)
                    .transferRequestId(it.requestId)
                    .customerName(it.data?.customer?.name)
                    .fee(it.data?.fee ?: it.amount?.let { getFee(it) })
                    .payoutTime(it.data?.payoutTime)
                    .creationTime(it.data?.creationTime)
                    .inDispute(it.inDispute)
            })
            .continuationToken(continuationToken?.encodeToken(objectMapper, paginationSecret))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.ADMIN)
    }
}
