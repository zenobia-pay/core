package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.MetricHelper
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
import com.zenobiapay.api.model.exception.ConcurrentModificationException
import com.zenobiapay.api.model.exception.DeclinedException
import com.zenobiapay.api.model.exception.InsufficientFundsException
import com.zenobiapay.cryptography.util.isSignatureValid
import com.zenobiapay.events.model.PutItemMetadataQueueRecord
import com.zenobiapay.events.model.UpdateItemMetadataQueueRecord
import com.zenobiapay.orum.util.WaiterFailedException
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.plaid.model.SignalResult
import com.zenobiapay.table.bank.model.BankAccountItem
import com.zenobiapay.table.bank.model.BankPermissions
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.BankAccount
import com.zenobiapay.table.transfer.model.PaymentParticipantIdentity
import com.zenobiapay.table.transfer.model.Signature
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.transfer.di.AVAILABLE_BALANCE_BUFFER
import com.zenobiapay.transfer.di.TRANSFER_METADATA_QUEUE_URL
import com.zenobiapay.transfer.model.FulfillTransferRequestMixin
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.services.sqs.SqsClient
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
    private val metricHelper: MetricHelper,
    private val sqsClient: SqsClient,
    @Named(TRANSFER_METADATA_QUEUE_URL)
    private val transferMetadataQueueUrl: String,
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
        logger.info { "Transfer request id: $transferRequestId"}
        val bankAccountId = request.bankAccountId
        userId!!
        var transferRequestItem = transferDao.getTransfer(transferRequestId = transferRequestId)
            ?: throw ResourceNotFoundException("TRANSFER")
        logger.info { "Got transfer request item $transferRequestItem" }
        if (transferRequestItem.outboundStatus != OutboundTransferStatus.NOT_STARTED) {
            logger.info { "Got state ${transferRequestItem.outboundStatus}, invalid state!"}
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

        val customerItem = userDao.getUserItem(userId) ?: throw ResourceNotFoundException("CUSTOMER")

        validateRequestSignature(request, customerBankAccountItem)

        val transferAmount = transferRequestItem.amount!!
        val transferRequestData = transferRequestItem.data!!
        val debtorId = transferRequestData.merchant!!
        val creditorId = PaymentParticipantIdentity(
            id = userId,
            bankAccountId = bankAccountId,
            name = "${customerItem.data.firstName} ${customerItem.data.lastName}"
        )
        val fulfillRequestId = input.requestContext.requestId

        val shouldPreApprove = shouldPreApprove(transferAmount, customerBankAccountItem.accessToken, bankAccountId, transferRequestId, userId)

        transferRequestItem = try {
            transferDao.updateTransferRequestLocked(transferRequestItem)
        } catch (e: ConditionalCheckFailedException) {
            logger.error(e) {
                "Got conditional check error when locking transfer request. Failing fast."
            }
            throw TransferStatusException("Transfer status is no longer in NOT_STARTED state.")
        }

        logger.info { "Successfully set request to FULFILL_LOCKED" }
        val fulfillTimestamp = Instant.now()
        transferFunds(
            transferRequestId,
            transferAmount,
            creditorId
        )

        val statementItems = transferRequestData.statementItems.map { it.toApiStatementItem() }
        transferDao.updateTransferRequestFulfilled(
            preApproved = shouldPreApprove,
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

        logger.info { "Updated transfer request to fulfilled" }
        metricHelper.putMetric("TransactionAmount", transferAmount.toDouble(), mapOf())

        sqsClient.sendMessage {
            it.queueUrl(transferMetadataQueueUrl)
            it.messageBody(
                objectMapper.writeValueAsString(
                    UpdateItemMetadataQueueRecord(
                        transferRequestId = transferRequestId,
                        ownershipTime = Instant.now(),
                        ownerId = userId,
                    )
                )
            )
        }

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

    private fun shouldPreApprove(transferAmount: Int, accessToken: String, bankAccountId: String, transferRequestId: String, sub: String): Boolean {
        // TODO: RE_ENABLE PLAID SIGNAL
//        val signalResult = plaidWrapper.getRiskDecision(accessToken, bankAccountId, transferRequestId, transferAmount, sub)
//        logger.info { "Got signal result $signalResult" }
//        if (signalResult == SignalResult.DENY) throw DeclinedException()

        try {
            runBlocking {
                withTimeout(15.seconds) {
                    logger.info { "Checking balance" }
                    val balance = plaidWrapper.getAvailableBalance(accessToken, bankAccountId)
                    logger.info { "Got balance $balance" }
                    if (transferAmount * availableBalanceBuffer > balance) {
                        logger.info { "Balance $balance was not greater than transfer amount $transferAmount with buffer $availableBalanceBuffer"}
                        throw InsufficientFundsException()
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
//            logger.info { "Failed to fetch available funds for $bankAccountId. Returning signal result $signalResult" }
            metricHelper.putMetric("PlaidBalanceGetTimeout", 1.0, mapOf("path" to "/fulfill-transfer"))
        }
        // TODO: re-enable
//        return signalResult == SignalResult.ACCEPT
        return false
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
