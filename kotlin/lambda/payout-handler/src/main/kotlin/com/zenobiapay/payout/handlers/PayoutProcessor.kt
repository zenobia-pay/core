package com.zenobiapay.payout.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.events.model.EventBridgeEvent
import com.zenobiapay.events.model.SqsEvent
import com.zenobiapay.events.util.EventBridgeEventSerializer
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.OrumCreateTransferRequest
import com.zenobiapay.orum.model.TransferParticipant
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.payout.di.DaggerAppComponent
import com.zenobiapay.table.transfer.dao.PAYOUT_PREFIX
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.util.getFee
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.UserType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.logging.log4j.ThreadContext
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
        logger.info { "Got event ${objectMapper.writeValueAsString(event)}" }
        val request = eventBridgeEventSerializer.getEventBridgeEvent(event)
        if (request == null) {
            logger.info { "Request is empty, returning early" }
            return
        }

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

        try {
            newImage?.requestId.let { ThreadContext.put("transferId", it) }
            logger.info { "Got transfer request id ${newImage?.requestId}" }
            if (shouldPayout(
                    oldImage?.inboundStatus,
                    newImage?.inboundStatus,
                    oldImage?.outboundStatus,
                    newImage?.outboundStatus
                )
            ) {
                logger.info { "Paying out merchant" }
                val transferItem = transferDao.getTransfer(newImage!!.requestId)
                    ?: throw Error("Could not find transfer item ${newImage.requestId}")
                fulfillPayout(transferItem)
            }
        } finally {
            ThreadContext.clearAll()
        }
    }

    private fun shouldPayout(
        oldInboundStatus: InboundTransferStatus?,
        newInboundStatus: InboundTransferStatus?,
        oldOutboundStatus: OutboundTransferStatus?,
        newOutboundStatus: OutboundTransferStatus?,
    ): Boolean {
        val inboundStatusChanged = oldInboundStatus != newInboundStatus
        val outboundStatusChanged = oldOutboundStatus != newOutboundStatus

        if (inboundStatusChanged) {
            logger.info { "Inbound status has changed to $newInboundStatus"}
            return newInboundStatus == InboundTransferStatus.SETTLED
        } else if (outboundStatusChanged) {
            logger.info { "Outbound status has changed to $newOutboundStatus"}
            return newOutboundStatus == OutboundTransferStatus.IN_FLIGHT
        } else {
            logger.info { "inbound, outbound status has not been updated from $oldInboundStatus, $oldOutboundStatus"}
            return false
        }.also {
            logger.info { "Got shouldPayout: $it"}
        }
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
                transferReferenceId = "$PAYOUT_PREFIX#${transferItem.requestId}",
                amount = merchantPayout,
                destination = TransferParticipant(
                    customerReferenceId = generateMerchantOrumId(merchantId),
                    accountReferenceId = bankAccountId,
                    statementDisplayName = merchantData.data.merchantData?.displayName?.filter { it.isLetterOrDigit() }?.take(10) ?: "ZenobiaPay"
                )
            )
        )

        logger.info { "Payout complete. Marking transfer as paid out." }
        transferDao.updateTransferPaidOut(transferItem, fee, transferResponse.transfer.id, version = transferItem.version!! + 1)
    }
}
