package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.BankDao
import com.zenobiapay.generated.models.ListBankAccounts200Response
import com.zenobiapay.generated.models.ListBankAccounts200ResponseItemsInner
import com.zenobiapay.model.api.bank.ListBanksRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import javax.inject.Inject

class ListBanksOperation @Inject constructor(private val objectMapper: ObjectMapper, private val bankDao: BankDao) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        context.logger.log("Got path parameter keys ${input.pathParameters?.keys}")
        val request = ListBanksRequest.from(input.pathParameters, objectMapper)
        context.logger.log("Got request $request")

        val bankItems = bankDao.listBankAccounts(userId, request.continuationToken)
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
