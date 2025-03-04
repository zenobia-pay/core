package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.BankDao
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.api.transfer.FulfillTransferRequest
import com.zenobiapay.model.api.transfer.FulfillTransferResponse
import com.zenobiapay.model.api.transfer.Debtor
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.model.ddb.transfer.*
import com.zenobiapay.model.exception.TransferFailedException
import com.zenobiapay.model.exception.TransferStatusException
import com.zenobiapay.model.orum.OrumCreateTransferRequest
import com.zenobiapay.model.orum.OrumCreateTransferResponse
import com.zenobiapay.model.orum.TransferParticipant
import com.zenobiapay.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class FulfillTransferOperation @Inject constructor(
    private val orumUtil: OrumUtil,
    private val transferDao: TransferDao,
    private val bankDao: BankDao,
    private val objectMapper: ObjectMapper,
    private val cognitoUtil: CognitoUtil,
): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): ApiResponse {
        val request = FulfillTransferRequest.from(input.body, objectMapper)
        val date = getUtcDate().also { logger.info { "Using date $it" } }
        val transferRequestItem = transferDao.getTransferRequest(merchantId = request.debtorId, transferRequestId = request.transferRequestId)
        if (transferRequestItem.status != TransferStatus.NOT_STARTED) {
            throw TransferStatusException("Transfer status is no longer in NOT_STARTED state.")
        }
        logger.info { "Fetching bank item from userId $userId, accountId ${request.accountId}" }
        val bankItem = bankDao.getBankAccount(userId, request.accountId)
        val transferAmount = transferRequestItem.amount!!
        val transferRequestData = transferRequestItem.data!!
        val debtorId = transferRequestData.merchant!!
        val creditorId = PaymentParticipantIdentity(
            id = userId,
            name = cognitoUtil.getUserFullName(userId),
            bankAccountId = request.accountId,
        )
        val transferRequestId = request.transferRequestId
        val fulfillRequestId = input.requestContext.requestId

        val fulfillTimestamp = Instant.now()
        transferFunds(
            transferRequestId,
            transferAmount,
            creditorId
        )

        transferDao.addPayoutItemAmount(debtorId.id, transferAmount, date)
        val statementItems = transferRequestData.statementItems.map { it.toApiStatementItem() }
        updateTransferTableStatusSuccess(
            creditorId = creditorId,
            fulfillRequestId = fulfillRequestId,
            transferItem = transferRequestItem,
            timestamp = fulfillTimestamp,
        )

        return FulfillTransferResponse(
            amount = transferAmount,
            statementItems = statementItems,
            debtor = Debtor(
                id = debtorId.id,
                name = debtorId.name
            ),
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }

    private fun transferFunds(
        transferRequestId: String,
        transferAmount: Int,
        creditorId: PaymentParticipantIdentity,
    ): OrumCreateTransferResponse? {
        if (transferAmount == 0) {
            logger.info { "Transfer amount is 0. Skipping deduction." }
            return null
        }
        try {
            return orumUtil.createTransfer(
                OrumCreateTransferRequest(
                    transferReferenceId = transferRequestId,
                    amount = transferAmount,
                    source = TransferParticipant(
                        customerReferenceId = creditorId.id,
                        accountReferenceId = creditorId.bankAccountId,
                        statementDisplayName = creditorId.name
                    ),
                    destination = null,
                )
            )

        } catch (e: WaiterFailedException) {
            throw TransferFailedException()
        }
    }

    private fun updateTransferTableStatusSuccess(
        creditorId: PaymentParticipantIdentity,
        fulfillRequestId: String,
        transferItem: TransferItem,
        timestamp: Instant,
    ) {
        logger.info { "Updating DDB with transfer fulfill details" }
        transferDao.updateTransferRequest(
            transferItem = transferItem,
            fulfillRequestId = fulfillRequestId,
            customerIdentity = creditorId,
            timestamp = timestamp,
        )
    }
}