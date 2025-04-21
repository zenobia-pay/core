package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.DeleteBankAccountRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.table.bank.dao.BankDao
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class DeleteBankAccountOperation @Inject constructor(
    private val bankDao: BankDao,
    private val plaidWrapper: PlaidWrapper,
    private val orumWrapper: OrumWrapper,
): Operation<DeleteBankAccountRequest, EmptyApiResponse>() {
    override val inputType = DeleteBankAccountRequest::class.java

    override fun run(
        request: DeleteBankAccountRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        userId!!
        val item = try {
            bankDao.getBankAccount(userId, request.bankAccountId, request.deviceId)
        } catch (e: ResourceNotFoundException) {
            throw com.zenobiapay.api.model.exception.ResourceNotFoundException("BANK_ACCOUNT")
        }
        logger.info { "Removing plaid item" }
        plaidWrapper.removeItem(item.accessToken)
        logger.info { "Closing orum account" }
        orumWrapper.closeExternalAccount(item.data.orumId)
        logger.info { "Marking bank metadata as deleted" }

        bankDao.deleteBankAccount(userId, request.bankAccountId, request.deviceId)
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT, UserPoolGroup.CUSTOMER)
    }
}