package com.zenobiapay.transfertableevent.logic

import com.zenobia.metric.MetricHelper
import com.zenobiapay.api.generated.model.TransferStatus
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.transfertableevent.util.EmailUtil
import com.zenobiapay.transfertableevent.util.WebhookUtil
import com.zenobiapay.transfertableevent.util.WebsocketUtil
import com.zenobiapay.webhook.util.isValidWebhook
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventLogic @Inject constructor(
    private val webhookUtil: WebhookUtil,
    private val websocketUtil: WebsocketUtil,
    private val metricsHelper: MetricHelper,
    private val userDao: UserDao,
    private val emailUtil: EmailUtil,
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
        val merchantId = newItem.data!!.merchant!!.id
        var isWebhookStatusSuccessful = true
        if (webhookUrl != null) {
            if (isValidWebhook(webhookUrl)) {
                val response = webhookUtil.sendTransferStatus(
                    webhookUrl,
                    merchantId,
                    newItem.requestId,
                    status,
                    newItem.amount!!
                )
                if (response == null || !response.isSuccessful) {
                    logger.warn { "Invalid webhook attempted to publish. Skipping" }
                    isWebhookStatusSuccessful = false
                }
            } else {
                logger.warn { "Invalid webhook attempted to publish. Skipping" }
                metricsHelper.putMetric("InvalidWebhook", 1.0)
                isWebhookStatusSuccessful = false
            }
        }
        // Note: webhook needs to send before websocket to ensure merchant backend is notified first.
        websocketUtil.sendWebsocketUpdate(
            newItem.requestId,
            merchantId,
            status,
            newItem.data?.customer!!.name,
        )

        if (!isWebhookStatusSuccessful) {
            metricsHelper.putMetric("WebhookSendFailure", 1.0)
            throw Exception("Failed to send webhook status.")
        }

        val merchantItem = userDao.getUserItem(newItem.data?.merchant!!.id)
        merchantItem?.data?.merchantData?.notificationEmail?.let {
            logger.info { "Got notification email $it. Sending email"}
            emailUtil.sendEmail(
                it,
                merchantItem.data!!.merchantData!!.displayName!!,
                newItem.data!!.customer!!.name!!,
                newItem.requestId,
                newItem.outboundStatus,
            )
        }
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
