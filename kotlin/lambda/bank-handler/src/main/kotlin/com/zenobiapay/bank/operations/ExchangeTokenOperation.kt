package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.model.AccountSubtype
import com.zenobiapay.api.model.ApiResponse
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.bank.ExchangeTokenRequest
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.OrumCreateExternalAccountRequest
import com.zenobiapay.plaid.PlaidWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class ExchangeTokenOperation @Inject constructor(
    private val plaidWrapper: PlaidWrapper,
    private val orumWrapper: OrumWrapper,
    private val objectMapper: ObjectMapper,
    private val bankDao: BankDao
) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ApiResponse {
        logger.info { "Got input body ${input.body}" }
        val request = ExchangeTokenRequest.from(input.body, objectMapper)
        val exchangeResponse = plaidWrapper.exchangeLinkToken(userId!!, request.linkToken)
        logger.info { "Got exchange response $exchangeResponse" }

        val accountsToAch = plaidWrapper.getZippedAccountsAndAch(exchangeResponse.accessToken)

        accountsToAch.forEach { (account, ach) ->
            logger.info { "Processing account $account, ach $ach" }
            if (ach == null) {
                logger.error { "Could not find ach number for account ${account.accountId}" }
                throw InvalidRequestException("Could not find ach number for provided account")
            }
            if (account.subtype != AccountSubtype.CHECKING && account.subtype != AccountSubtype.SAVINGS) {
                logger.error { "expected checking or savings, got subtype ${account.subtype} for account ${account.accountId}" }
                throw InvalidRequestException("Provided account is not checking nor savings")
            }

            val orumId = orumWrapper.createExternalOrganization(
                OrumCreateExternalAccountRequest(
                    accountReferenceId = ach.accountId,
                    customerReferenceId = userId,
                    customerResourceType = "person", // TODO: use enum
                    accountType = account.subtype!!.value,
                    accountNumber = ach.account,
                    routingNumber = ach.routing,
                    accountHolderName = "John Doe" // TODO: pass real user name
                )
            ).externalAccount.id

            bankDao.putBankAccount(
                userId = userId,
                plaidItemId = exchangeResponse.itemId,
                bankAccountId = account.accountId,
                bankAccountName = account.name,
                token = exchangeResponse.accessToken,
                bankAccountType = account.subtype!!.value,
                orumId = orumId
            )
            context.logger.log("Successfully wrote to ddb bank item ${account.accountId}, orum id $orumId")
        }

        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
