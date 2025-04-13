package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.BankAccount
import com.zenobiapay.api.generated.model.ListCustomerTransfers200Response
import com.zenobiapay.api.generated.model.ListCustomerTransfers200ResponseItemsInner
import com.zenobiapay.api.generated.model.ListCustomerTransfersRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.table.model.BadTokenException
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.transfer.di.PAGINATION_SECRET
import javax.inject.Inject
import javax.inject.Named

class ListCustomerTransfersOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao,
    @Named(PAGINATION_SECRET) private val paginationSecret: String,
) : Operation<ListCustomerTransfersRequest, ListCustomerTransfers200Response>() {
    override val inputType = ListCustomerTransfersRequest::class.java
    override fun run(
        request: ListCustomerTransfersRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListCustomerTransfers200Response {
        val (transfers, continuationToken) = try {
            transferDao.listCustomerTransfers(userId!!, request.continuationToken, paginationSecret)
        } catch (e: BadTokenException) {
            throw InvalidRequestException("Bad token")
        }
        return ListCustomerTransfers200Response()
            .items(
                transfers.map {
                    ListCustomerTransfers200ResponseItemsInner()
                        .customerBankAccount(
                            BankAccount()
                                .bankAccountName(it.data?.customerBankAccount?.name)
                                .lastFourDigits(it.data?.customerBankAccount?.lastFourDigits)
                        )
                        .amount(it.amount)
                        .status(it.status.toApiTransferStatus())
                        .merchant(it.data!!.merchant!!.toApiParticipantIdentity())
                        .creationTime(it.data!!.creationTime)
                },
            ).continuationToken(continuationToken?.encodeToken(objectMapper, paginationSecret))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}
