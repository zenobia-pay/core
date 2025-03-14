package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.BankDao
import com.zenobiapay.dao.TransferDao
import com.zenobiapay.dao.UserDao
import com.zenobiapay.generated.models.FulfillTransfer200Response
import com.zenobiapay.generated.models.FulfillTransferRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.model.ddb.transfer.PaymentParticipantIdentity
import com.zenobiapay.model.ddb.transfer.TransferItem
import com.zenobiapay.model.ddb.transfer.TransferStatus
import com.zenobiapay.model.exception.InvalidRequestException
import com.zenobiapay.model.exception.ResourceNotFoundException
import com.zenobiapay.model.exception.TransferFailedException
import com.zenobiapay.model.exception.TransferStatusException
import com.zenobiapay.model.orum.OrumCreateTransferRequest
import com.zenobiapay.model.orum.OrumCreateTransferResponse
import com.zenobiapay.model.orum.TransferParticipant
import com.zenobiapay.operation.Operation
import com.zenobiapay.util.CognitoUtil
import com.zenobiapay.util.OrumUtil
import com.zenobiapay.util.WaiterFailedException
import com.zenobiapay.util.getUtcDate
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class FulfillTransferOperation @Inject constructor(
    private val orumUtil: OrumUtil,
    private val transferDao: TransferDao,
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper,
    private val cognitoUtil: CognitoUtil
) : Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): FulfillTransfer200Response {
        val request = objectMapper.readValue(input.body, FulfillTransferRequest::class.java)
        val transferRequestId = request.transferRequestId ?: throw InvalidRequestException("Parameter transferRequestId not passed")
        val merchantId = request.merchantId ?: throw InvalidRequestException("Parameter merchantId not passed")
        val bankAccountId = request.bankAccountId ?: throw InvalidRequestException("Parameter bankAccountId not passed")

        val date = getUtcDate().also { logger.info { "Using date $it" } }
        val transferRequestItem = transferDao.getMerchantTransfer(merchantId = merchantId, transferRequestId = transferRequestId)
        if (transferRequestItem.status != TransferStatus.NOT_STARTED) {
            throw TransferStatusException("Transfer status is no longer in NOT_STARTED state.")
        }
        logger.info { "Fetching bank item from userId $userId, accountId $bankAccountId" }
        bankDao.getBankAccount(userId, bankAccountId) ?: throw ResourceNotFoundException("BANK_ACCOUNT")
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
            merchant = com.zenobiapay.generated.models.PaymentParticipantIdentity(
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
            return orumUtil.createTransfer(
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
