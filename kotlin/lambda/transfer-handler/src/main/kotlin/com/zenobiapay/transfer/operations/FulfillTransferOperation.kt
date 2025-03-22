package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.api.generated.models.FulfillTransfer200Response
import com.zenobiapay.api.generated.models.FulfillTransferRequest
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.orum.model.OrumCreateTransferRequest
import com.zenobiapay.orum.model.OrumCreateTransferResponse
import com.zenobiapay.orum.model.TransferParticipant
import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.api.exception.ResourceNotFoundException
import com.zenobiapay.api.exception.TransferFailedException
import com.zenobiapay.api.exception.TransferStatusException
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.cognito.CognitoUtil
import com.zenobiapay.orum.util.WaiterFailedException
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.PaymentParticipantIdentity
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.model.TransferStatus
import com.zenobiapay.table.user.dao.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class FulfillTransferOperation @Inject constructor(
    private val orumWrapper: OrumWrapper,
    private val transferDao: TransferDao,
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper,
    private val cognitoUtil: CognitoUtil
) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String?): FulfillTransfer200Response {
        val request = objectMapper.readValue(input.body, FulfillTransferRequest::class.java)
        val transferRequestId = request.transferRequestId
        val merchantId = request.merchantId
        val bankAccountId = request.bankAccountId

        val date = LocalDate.now(ZoneOffset.UTC).also { logger.info { "Using date $it" } }
        val transferRequestItem = transferDao.getMerchantTransfer(merchantId = merchantId, transferRequestId = transferRequestId)
        if (transferRequestItem.status != TransferStatus.NOT_STARTED) {
            throw TransferStatusException("Transfer status is no longer in NOT_STARTED state.")
        }
        logger.info { "Fetching bank item from userId $userId, accountId $bankAccountId" }
        bankDao.getBankAccount(userId!!, bankAccountId) ?: throw ResourceNotFoundException("BANK_ACCOUNT")
        val merchantItem = userDao.getMerchant(merchantId) ?: throw ResourceNotFoundException("MERCHANT")

        val transferAmount = transferRequestItem.amount!!
        val transferRequestData = transferRequestItem.data!!
        val debtorId = transferRequestData.merchant!!
        val creditorId = PaymentParticipantIdentity(
            id = userId,
            name = cognitoUtil.getUserFullName(userId),
            bankAccountId = bankAccountId
        )
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
            webhookUrl = merchantItem.data.webhookUrl
        )

        return FulfillTransfer200Response(
            amount = transferAmount,
            statementItems = statementItems,
            merchant = com.zenobiapay.api.generated.models.PaymentParticipantIdentity(
                id = debtorId.id,
                name = debtorId.name
            )
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }

    private fun transferFunds(
        transferRequestId: String,
        transferAmount: Int,
        creditorId: PaymentParticipantIdentity
    ): OrumCreateTransferResponse? {
        if (transferAmount == 0) {
            logger.info { "Transfer amount is 0. Skipping deduction." }
            return null
        }
        try {
            return orumWrapper.createTransfer(
                OrumCreateTransferRequest(
                    transferReferenceId = transferRequestId,
                    amount = transferAmount,
                    source = TransferParticipant(
                        customerReferenceId = creditorId.id,
                        accountReferenceId = creditorId.bankAccountId,
                        statementDisplayName = creditorId.name
                    ),
                    destination = null
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
        webhookUrl: String?
    ) {
        logger.info { "Updating DDB with transfer fulfill details" }
        transferDao.updateTransferRequest(
            transferItem = transferItem,
            fulfillRequestId = fulfillRequestId,
            customerIdentity = creditorId,
            timestamp = timestamp,
            webhookUrl = webhookUrl
        )
    }
}
