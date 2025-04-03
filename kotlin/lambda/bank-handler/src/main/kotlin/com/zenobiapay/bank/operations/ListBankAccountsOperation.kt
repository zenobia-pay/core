package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.models.ListBankAccounts200Response
import com.zenobiapay.api.generated.models.ListBankAccounts200ResponseItemsInner
import com.zenobiapay.api.generated.models.ListBankAccountsRequest
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.bank.dao.BankDao
import javax.inject.Inject

class ListBankAccountsOperation @Inject constructor(private val objectMapper: ObjectMapper, private val bankDao: BankDao) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): Any {
        val request = objectMapper.readValue(input.body, ListBankAccountsRequest::class.java)
        context.logger.log("Got request $request")

        val bankItems = bankDao.listBankAccounts(userId!!, request.deviceId, request.continuationToken)
        context.logger.log("Got bank items $bankItems")
        return ListBankAccounts200Response(
            items = bankItems.map {
                ListBankAccounts200ResponseItemsInner(
                    bankAccountId = it.data.bankAccountId,
                    bankAccountName = it.data.bankAccountName
                )
            }
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
