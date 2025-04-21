package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.model.exception.InvalidSignatureException
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.api.generated.model.FulfillTransfer200Response
import com.zenobiapay.api.generated.model.FulfillTransferRequest
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.orum.model.OrumCreateTransferRequest
import com.zenobiapay.orum.model.OrumCreateTransferResponse
import com.zenobiapay.orum.model.TransferParticipant
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.model.exception.TransferFailedException
import com.zenobiapay.api.model.exception.TransferStatusException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.BalanceTimeOutException
import com.zenobiapay.api.model.exception.ConcurrentModificationException
import com.zenobiapay.api.model.exception.InsufficientFundsException
import com.zenobiapay.cryptography.util.isSignatureValid
import com.zenobiapay.orum.util.WaiterFailedException
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.table.bank.model.BankAccountItem
import com.zenobiapay.table.bank.model.BankPermissions
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.BankAccount
import com.zenobiapay.table.transfer.model.PaymentParticipantIdentity
import com.zenobiapay.table.transfer.model.Signature
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.transfer.di.AVAILABLE_BALANCE_BUFFER
import com.zenobiapay.transfer.model.FulfillTransferRequestMixin
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class FulfillTransferOperation @Inject constructor(
    private val orumWrapper: OrumWrapper,
    private val plaidWrapper: PlaidWrapper,
    private val transferDao: TransferDao,
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper,
    @Named(AVAILABLE_BALANCE_BUFFER) private val availableBalanceBuffer: Double,
) : Operation<FulfillTransferRequest, FulfillTransfer200Response>() {

    override val inputType = FulfillTransferRequest::class.java

    private val mixinObjectMapper = lazy {
        objectMapper.copy()
            .addMixIn(FulfillTransferRequest::class.java, FulfillTransferRequestMixin::class.java)
    }

    override fun run(
        request: FulfillTransferRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): FulfillTransfer200Response {
        val transferRequestId = request.transferRequestId
        val bankAccountId = request.bankAccountId
        userId!!

        val date = LocalDate.now(ZoneOffset.UTC).also { logger.info { "Using date $it" } }
        var transferRequestItem = transferDao.getTransfer(transferRequestId = transferRequestId)
            ?: throw ResourceNotFoundException("TRANSFER")
        logger.info { "Got transfer request item $transferRequestItem" }
        if (transferRequestItem.outboundStatus != OutboundTransferStatus.NOT_STARTED) {
            throw TransferStatusException("Transfer status is no longer in NOT_STARTED state.")
        }

        logger.info { "Fetching bank item from userId $userId, accountId $bankAccountId" }
        val customerBankAccountItem = try {
            bankDao.getBankAccount(userId, bankAccountId, request.deviceId)
        } catch (e: software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException) {
            logger.info { "Could not find bank id $bankAccountId" }
            throw ResourceNotFoundException("BANK_ACCOUNT")
        }

        if (customerBankAccountItem.data.bankPermissions != BankPermissions.SEND_ONLY) {
            throw InvalidRequestException("Bank account does not have permission to send funds.")
        }

        val merchantItem = transferRequestItem.data?.merchant?.id?.let {
            userDao.getUserItem(transferRequestItem.data!!.merchant!!.id)
        } ?: throw ResourceNotFoundException("MERCHANT")

        validateRequestSignature(request, customerBankAccountItem)

        val transferAmount = transferRequestItem.amount!!
        val transferRequestData = transferRequestItem.data!!
        val debtorId = transferRequestData.merchant!!
        val creditorId = PaymentParticipantIdentity(
            id = userId,
            bankAccountId = bankAccountId
        )
        val fulfillRequestId = input.requestContext.requestId

        assertHasAvailableFunds(transferAmount, customerBankAccountItem.accessToken, bankAccountId)

        transferRequestItem = try {
            transferDao.updateTransferRequestLocked(transferRequestItem)
        } catch (e: ConditionalCheckFailedException) {
            throw ConcurrentModificationException()
        }

        logger.info { "Successfully set request to IN_FLIGHT" }
        val fulfillTimestamp = Instant.now()
        transferFunds(
            transferRequestId,
            transferAmount,
            creditorId
        )

        transferDao.addPayoutItemAmount(debtorId.id, transferAmount, date)
        logger.info { "Added payout item" }
        val statementItems = transferRequestData.statementItems.map { it.toApiStatementItem() }
        transferDao.updateTransferRequestFulfilled(
            transferItem = transferRequestItem,
            fulfillRequestId = fulfillRequestId,
            customerIdentity = creditorId,
            timestamp = fulfillTimestamp,
            webhookUrl = merchantItem.data.merchantData?.webhookUrl,
            customerBankAccount = BankAccount(
                name = customerBankAccountItem.data.bankAccountName,
                id = customerBankAccountItem.data.bankAccountId,
                lastFourDigits = customerBankAccountItem.data.lastFourDigits,
            ),
            signature = Signature(
                signatureType = request.signature.signatureType.value,
                signature = request.signature.signatureValue
            ),
        )
        logger.info { "Updated transfer request" }

        return FulfillTransfer200Response()
            .amount(transferAmount)
            .statementItems(statementItems)
            .merchant(com.zenobiapay.api.generated.model.PaymentParticipantIdentity()
                .id(transferRequestItem.data?.merchant?.id)
                .name(merchantItem.data.merchantData?.displayName)
            )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }

    private fun validateRequestSignature(request: FulfillTransferRequest, bankAccountItem: BankAccountItem) {
        val body = mixinObjectMapper.value.writeValueAsString(request)
        val deviceCertificate = bankAccountItem.data.deviceCertificate

        if (deviceCertificate == null || bankAccountItem.data.bankPermissions != BankPermissions.SEND_ONLY) {
            throw InvalidRequestException("Bank account not allowed to send money")
        }
        val isValid = isSignatureValid(
            data = body.toByteArray(),
            certificate = deviceCertificate.certificateValue,
            base64Signature = request.signature.signatureValue,
            signatureType = request.signature.signatureType
        )
        if (!isValid) {
            throw InvalidSignatureException()
        }
    }

    private fun assertHasAvailableFunds(transferAmount: Int, accessToken: String, bankAccountId: String) {
        logger.info { "Checking balance" }
        val balance = try {
            runBlocking {
                withTimeout(20.seconds) {
                    plaidWrapper.getAvailableBalance(accessToken, bankAccountId)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw BalanceTimeOutException()
        }
        logger.info { "Got balance $balance" }
        if (transferAmount > balance * availableBalanceBuffer) {
            throw InsufficientFundsException()
        }
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
                        customerReferenceId = generateCustomerOrumId(creditorId.id),
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
}
