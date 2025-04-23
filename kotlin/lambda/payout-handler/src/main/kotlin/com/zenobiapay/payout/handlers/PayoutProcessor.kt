package com.zenobiapay.payout.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.model.ddb.transfer.PayoutId
import com.zenobiapay.orum.model.OrumCreateTransferRequest
import com.zenobiapay.orum.model.TransferParticipant
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.payout.di.DaggerAppComponent
import com.zenobiapay.payout.model.EventBridgeEvent
import com.zenobiapay.payout.model.PayoutMessage
import com.zenobiapay.payout.model.ScheduledEvent
import com.zenobiapay.payout.util.EventBridgeEventSerializer
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.util.getFee
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.UserType
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class MerchantNotConfiguredException: Exception("Merchant configuration not set!")

class PayoutProcessor : RequestHandler<Map<String, Any>, Unit> {

    @Inject
    lateinit var transferDao: TransferDao

    @Inject
    lateinit var orumWrapper: OrumWrapper

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var eventBridgeEventSerializer: EventBridgeEventSerializer

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: Map<String, Any>, context: Context?) {
//        logger.info { "Got event $event" }
        val request = objectMapper.convertValue(event, EventBridgeEvent::class.java)
        if (request.detail?.eventName != "MODIFY") {
            logger.info { "Request is not a modification, ignoring." }
            return
        }

        val oldImage = request.detail?.dynamodb?.oldImage?.let {
            TransferItem.fromAttributeValueMap(
                eventBridgeEventSerializer.parseAttributeValueMap(it)
            )
        }
        val newImage = request.detail?.dynamodb?.newImage?.let {
            TransferItem.fromAttributeValueMap(
                eventBridgeEventSerializer.parseAttributeValueMap(it)
            )
        }

        logger.info { "Got transfer request id ${newImage?.requestId}"}

        if (newImage?.outboundStatus != OutboundTransferStatus.IN_FLIGHT) {
            logger.info { "Request is not in flight. Skipping paying out"}
        }
        val transferItem = transferDao.getTransfer(newImage!!.requestId) ?: throw Error("Could not find transfer item ${newImage.requestId}")
        fulfillPayout(transferItem)
    }

    private fun fulfillPayout(transferItem: TransferItem) {
        if (transferItem.outboundStatus != OutboundTransferStatus.IN_FLIGHT) {
            logger.info { "Outbound transfer status is not in flight. Skipping paying out." }
        }

        val amount = transferItem.amount!!
        val fee = getFee(amount)
        val merchantPayout = amount - fee
        val merchantId = transferItem.data?.merchant?.id!!

        assert(merchantPayout > 0)
        val merchantData = userDao.getUserItem(merchantId)
        assert(merchantData?.userType == UserType.MERCHANT) {
            "Merchant data for merchant ${merchantData?.pk} does not exist or is not a merchant"
        }
        val merchantBankAccountId = merchantData?.data?.merchantData?.bankAccountId
        assert(merchantBankAccountId != null) { "Merchant bank account id is not specified" }

        // TODO: check the bank account is of correct type
        val bankAccountId = merchantData?.data?.merchantData?.bankAccountId ?: throw MerchantNotConfiguredException()

        logger.info { "Locking transfer payout" }
        transferDao.updateTransferPayoutLocked(transferItem)

        logger.info { "Sending orum payout response" }
        val transferResponse = orumWrapper.createTransfer(
            OrumCreateTransferRequest(
                transferReferenceId = "PAYOUT#${transferItem.requestId}",
                amount = merchantPayout,
                destination = TransferParticipant(
                    customerReferenceId = generateMerchantOrumId(merchantId),
                    accountReferenceId = bankAccountId,
                    statementDisplayName = merchantData.data.merchantData?.displayName?.filter { it.isLetterOrDigit() }?.take(10) ?: "ZenobiaPay"
                )
            ),
            waitForCompletedState = true
        )

        logger.info { "Payout complete. Marking transfer as paid out." }
        transferDao.updateTransferPaidOut(transferItem, fee, transferResponse.transfer.id, version = transferItem.version!! + 1)
    }
}
