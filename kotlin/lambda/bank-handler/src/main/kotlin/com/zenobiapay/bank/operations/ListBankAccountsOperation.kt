package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.BankAccount
import com.zenobiapay.api.generated.model.ListBankAccounts200Response
import com.zenobiapay.api.generated.model.ListBankAccountsRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.bank.di.PAGINATION_SECRET
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.table.model.BadTokenException
import com.zenobiapay.table.model.ContinuationToken
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class ListBankAccountsOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val bankDao: BankDao,
    @Named(PAGINATION_SECRET) private val paginationSecret: String,
): Operation<ListBankAccountsRequest, ListBankAccounts200Response>() {

    override val inputType = ListBankAccountsRequest::class.java

    override fun run(request: ListBankAccountsRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ListBankAccounts200Response {
        logger.info { "Got request $request" }

        val decodedToken = request.continuationToken?.let {
            try {
                ContinuationToken.decodeToken(it, objectMapper, paginationSecret)
            } catch (e: BadTokenException) {
                throw InvalidRequestException("Bad token")
            }
        }
        val (bankItems, continuationToken) = bankDao.listBankAccounts(
            userId!!,
            request.deviceId,
            decodedToken
        )
        logger.info { "Got bank item count ${bankItems.size}" }
        return ListBankAccounts200Response().items(
            bankItems.map {
                BankAccount()
                    .bankAccountId(it.data.bankAccountId)
                    .bankAccountName(it.data.bankAccountName)
                    .lastFourDigits(it.data.lastFourDigits)
            }
        ).continuationToken(continuationToken?.encodeToken(objectMapper, paginationSecret))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
