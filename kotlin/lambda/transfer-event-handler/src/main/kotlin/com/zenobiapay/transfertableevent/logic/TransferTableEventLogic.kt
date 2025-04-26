package com.zenobiapay.transfertableevent.logic

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
        val status = newItem.inboundStatus
        val requestId = newItem.requestId
        val transferCompleted = shouldSendStatus(oldItem, newItem)
        if (transferCompleted && webhookUrl != null) {
            logger.info { "Sending status $status for request id $requestId to webhook $webhookUrl" }
            if (isValidWebhook(webhookUrl)) {
                webhookUtil.sendTransferStatus(
                    webhookUrl,
                    newItem.data?.merchant?.id!!,
                    newItem.requestId,
                    newItem.outboundStatus.toApiTransferStatus(),
                    newItem.amount!!
                )
            } else {
                logger.warn { "Invalid webhook attempted to publish. Skipping" }
            }
        }
        if (transferCompleted) {
            websocketUtil.sendWebsocketUpdate(
                newItem.requestId,
                newItem.data?.merchant!!.id,
                newItem.outboundStatus.toApiTransferStatus()
            )
        }
    }

    /**
     * Sends out status of transfer. If completed, they immediately have the funds.
     * If in flight waiting, we send in flight and they should wait for funds before sending merchandise.
     */
    private fun shouldSendStatus(oldItem: TransferItem, newItem: TransferItem): Boolean {
        val oldItemOutboundStatus = oldItem.outboundStatus
        val newItemOutboundStatus = newItem.outboundStatus

        if (oldItemOutboundStatus == newItemOutboundStatus) {
            logger.info { "No change in outbound status. Ignoring." }
        }

        return newItemOutboundStatus == OutboundTransferStatus.COMPLETED ||
                newItemOutboundStatus == OutboundTransferStatus.IN_FLIGHT_WAITING.also {
                    logger.info { "For status $newItemOutboundStatus, shouldSendStatus = $it" }
        }
    }
}
