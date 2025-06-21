package com.zenobiapay.payout.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.MetricHelper
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
import jakarta.inject.Inject

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
    lateinit var metricHelper: MetricHelper

    @Inject
    lateinit var eventBridgeEventSerializer: EventBridgeEventSerializer

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: Map<String, Any>, context: Context?) {
        logger.info { "Got payout event ${objectMapper.writeValueAsString(event)}" }
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
                metricHelper.emitSuccessMetric("PayoutSuccess", mapOf()) {
                    fulfillPayout(transferItem)
                }
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

        if (outboundStatusChanged) {
            logger.info { "Outbound status has changed to $newOutboundStatus"}
            return newOutboundStatus == OutboundTransferStatus.IN_FLIGHT_APPROVED
        } else if (inboundStatusChanged) {
            logger.info { "Inbound status has changed to $newInboundStatus, outbound status $newOutboundStatus"}
            return newInboundStatus == InboundTransferStatus.SETTLED && newOutboundStatus != OutboundTransferStatus.COMPLETED
        } else {
            logger.info { "inbound, outbound status has not been updated from $oldInboundStatus, $oldOutboundStatus"}
            return false
        }.also {
            logger.info { "Got shouldPayout: $it"}
        }
    }

    private fun fulfillPayout(transferItem: TransferItem) {
        val amount = transferItem.amount!!
        val fee = getFee(amount)
        val merchantPayout = amount - fee
        val merchantId = transferItem.data?.merchant?.id!!

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

        val transferResponse = if (merchantPayout > 0) {
            logger.info { "Sending orum payout" }
            orumWrapper.createTransfer(
                OrumCreateTransferRequest(
                    transferReferenceId = "$PAYOUT_PREFIX#${transferItem.requestId}",
                    amount = merchantPayout,
                    destination = TransferParticipant(
                        customerReferenceId = merchantData.data.orumReferenceId!!,
                        accountReferenceId = bankAccountId,
                        // TODO: make more descriptive display name
                        statementDisplayName = "Zenobia Pay"
                    )
                )
            )
        } else null.also {
            logger.info { "Skipping payout, payount <= 0" }
            metricHelper.putMetric("SkipPayout", 1.0, mapOf())
        }
        logger.info { "Payout complete. Marking transfer as paid out." }
        transferDao.updateTransferPaidOut(transferItem, fee, transferResponse?.transfer?.id, version = transferItem.version!! + 1)
        metricHelper.putMetric("MerchantPayout", merchantPayout.toDouble(), mapOf())
        metricHelper.putMetric("FeeCollected", fee.toDouble(), mapOf())
        metricHelper.putMetric("TotalPayout", amount.toDouble(), mapOf())
    }
}
