package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListBankAccounts200Response
import com.zenobiapay.api.generated.model.ListBankAccounts200ResponseItemsInner
import com.zenobiapay.api.generated.model.ListBankAccountsRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.table.model.ContinuationToken
import javax.inject.Inject

class ListBankAccountsOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val bankDao: BankDao
): Operation<ListBankAccountsRequest, ListBankAccounts200Response>() {

    override val inputType = ListBankAccountsRequest::class.java

    override fun run(request: ListBankAccountsRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ListBankAccounts200Response {
        context.logger.log("Got request $request")

        val (bankItems, continuationToken) = bankDao.listBankAccounts(
            userId!!,
            request.deviceId,
            request.continuationToken?.let { ContinuationToken.decodeToken(it, objectMapper) }
        )
        context.logger.log("Got bank items $bankItems")
        return ListBankAccounts200Response().items(
            bankItems.map {
                ListBankAccounts200ResponseItemsInner()
                    .bankAccountId(it.data.bankAccountId)
                    .bankAccountName(it.data.bankAccountName)
            }
        ).continuationToken(continuationToken?.encodeToken(objectMapper))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
