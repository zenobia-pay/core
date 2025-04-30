package com.zenobiapay.transfertableevent.logic

import com.zenobiapay.api.generated.model.TransferStatus
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.transfertableevent.util.WebhookUtil
import com.zenobiapay.transfertableevent.util.WebsocketUtil
import com.zenobiapay.webhook.util.isValidWebhook
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventLogic @Inject constructor(
    private val webhookUtil: WebhookUtil,
    private val websocketUtil: WebsocketUtil,
) {
    fun handleRecord(oldImage: TransferItem?, newImage: TransferItem?) {
        val hasOldImage = oldImage != null
        val hasNewImage = newImage != null

        if (!hasOldImage && hasNewImage) {
            logger.info { "Found new CREATE event" }
            logger.info { "No actions for create, skipping" }
        } else if (hasOldImage && hasNewImage) {
            logger.info { "Found new MODIFY event" }
            handleModifyEvent(oldImage, newImage)
        }
    }

    private fun handleModifyEvent(oldItem: TransferItem, newItem: TransferItem) {
        logger.info { "Processing PK value: ${newItem.pk}" }
        val webhookUrl = newItem.data!!.webhookUrl
        val requestId = newItem.requestId
        val status = getStatusToSend(oldItem, newItem)
        if (status == null) {
            logger.info { "No status to send. Skipping publishing update" }
            return
        }
        logger.info { "Sending status $status for requestId $requestId" }
        if (webhookUrl != null) {
            if (isValidWebhook(webhookUrl)) {
                webhookUtil.sendTransferStatus(
                    webhookUrl,
                    newItem.data?.merchant?.id!!,
                    newItem.requestId,
                    status,
                    newItem.amount!!
                )
            } else {
                logger.warn { "Invalid webhook attempted to publish. Skipping" }
            }
        }
        websocketUtil.sendWebsocketUpdate(
            newItem.requestId,
            newItem.data?.merchant!!.id,
            status
        )
    }

    /**
     * Sends out status of transfer. If completed, they immediately have the funds.
     * If in flight waiting, we send in flight and they should wait for funds before sending merchandise.
     */
    private fun getStatusToSend(oldItem: TransferItem, newItem: TransferItem): TransferStatus? {
        val oldItemOutboundStatus = oldItem.outboundStatus
        val newItemOutboundStatus = newItem.outboundStatus

        val oldItemInboundStatus = oldItem.inboundStatus
        val newItemInboundStatus = newItem.inboundStatus

        logger.info { "Got outbound status $oldItemOutboundStatus -> $newItemOutboundStatus, inbound status $oldItemInboundStatus -> $newItemInboundStatus" }

        if (oldItemOutboundStatus != newItemOutboundStatus) {
            val paymentComplete = newItemOutboundStatus == OutboundTransferStatus.COMPLETED
            val paymentWaiting = newItemOutboundStatus == OutboundTransferStatus.IN_FLIGHT_WAITING
            if (paymentComplete || paymentWaiting) {
                return newItemOutboundStatus.toApiTransferStatus().also {
                    logger.info { "Got status to return to customer $it" }
                }
            }
        }
        if (oldItemInboundStatus != newItemInboundStatus) {
            val paymentFailed = newItem.inboundStatus == InboundTransferStatus.FAILED && (newItemOutboundStatus != OutboundTransferStatus.COMPLETED)
            if (paymentFailed) {
                logger.info { "Got payment failed. Reporting to customer" }
                return TransferStatus.FAILED
            }
        }
        logger.info { "Did not find statuses to send. Returning null." }
        return null
    }
}
