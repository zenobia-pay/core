package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.model.AccountBase
import com.plaid.client.model.AccountSubtype
import com.plaid.client.model.ItemPublicTokenExchangeResponse
import com.plaid.client.model.NumbersACH
import com.zenobiapay.api.model.ApiResponse
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.generated.model.ExchangeTokenRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.util.getUserRole
import com.zenobiapay.cryptography.util.isCertificateValid
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.CustomerResourceType
import com.zenobiapay.orum.model.OrumCreateExternalAccountRequest
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.table.bank.model.DeviceCertificate
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class ExchangeTokenOperation @Inject constructor(
    private val plaidWrapper: PlaidWrapper,
    private val orumWrapper: OrumWrapper,
    private val objectMapper: ObjectMapper,
    private val bankDao: BankDao,
) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ApiResponse {
        userId!!
        logger.info { "Got input body ${input.body}" }
        val request = objectMapper.readValue(input.body, ExchangeTokenRequest::class.java)

        if (request.deviceCertificate != null && !isCertificateValid(
                request.deviceCertificate!!.certificateValue,
                request.deviceCertificate!!.certificateType
            )
        ) {
            throw InvalidRequestException("DEVICE_CERTIFICATE")
        }

        val exchangeResponse = plaidWrapper.exchangeLinkToken(request.linkToken)
        logger.info { "Got exchange response $exchangeResponse" }

        val accountsToAch = plaidWrapper.getZippedAccountsAndAch(exchangeResponse.accessToken)

        accountsToAch.forEach { (account, ach) -> processAccount(request, exchangeResponse, userId, input.requestContext.getUserRole(), account, ach) }

        return EmptyApiResponse()
    }

    private fun processAccount(
        request: ExchangeTokenRequest,
        exchangeResponse: ItemPublicTokenExchangeResponse,
        userId: String,
        userPoolGroup: UserPoolGroup,
        account: AccountBase,
        ach: NumbersACH?
    ) {
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
                customerReferenceId = getOrumCustomerId(userId, userPoolGroup),
                customerResourceType = getCustomerResourceType(userPoolGroup),
                accountType = account.subtype!!.value,
                accountNumber = ach.account,
                routingNumber = ach.routing,
                accountHolderName = "John Doe" // TODO: pass real user name
            )
        ).externalAccount.id

        val deviceCertificate = if (request.deviceCertificate != null) {
            DeviceCertificate(
                certificateType = request.deviceCertificate!!.certificateType!!.value,
                certificateValue = request.deviceCertificate!!.certificateValue!!,
            )
        } else null

        bankDao.putBankAccount(
            userId = userId,
            deviceId = request.deviceId,
            plaidItemId = exchangeResponse.itemId,
            bankAccountId = account.accountId,
            bankAccountName = account.name,
            token = exchangeResponse.accessToken,
            bankAccountType = account.subtype!!.value,
            orumId = orumId,
            deviceCertificate = deviceCertificate,
        )
        logger.info { "Successfully wrote to ddb bank item ${account.accountId}, orum id $orumId" }
    }

    private fun getCustomerResourceType(userPoolGroup: UserPoolGroup): CustomerResourceType {
        return when (userPoolGroup) {
            UserPoolGroup.MERCHANT -> CustomerResourceType.BUSINESS
            UserPoolGroup.CUSTOMER -> CustomerResourceType.PERSON
            UserPoolGroup.MERCHANT_M2M, UserPoolGroup.UNKNOWN -> throw Exception("Got invalid user pool group $userPoolGroup")
        }
    }

    private fun getOrumCustomerId(userId: String, userPoolGroup: UserPoolGroup): String {
        return when (userPoolGroup) {
            UserPoolGroup.MERCHANT -> generateMerchantOrumId(userId)
            UserPoolGroup.CUSTOMER -> generateCustomerOrumId(userId)
            UserPoolGroup.MERCHANT_M2M, UserPoolGroup.UNKNOWN -> throw Exception("Got invalid user pool group $userPoolGroup")
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
