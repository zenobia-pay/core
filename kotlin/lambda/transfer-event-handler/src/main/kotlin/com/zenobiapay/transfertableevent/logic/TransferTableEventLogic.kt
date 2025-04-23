package com.zenobiapay.transfertableevent.logic

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.model.TransferStatus
import com.zenobiapay.transfertableevent.util.WebhookUtil
import com.zenobiapay.transfertableevent.util.WebsocketUtil
import com.zenobiapay.webhook.util.isValidWebhook
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventLogic @Inject constructor(
    private val webhookUtil: WebhookUtil,
    private val websocketUtil: WebsocketUtil,
) {
    fun handleRecord(record: DynamodbEvent.DynamodbStreamRecord) {
        val hasOldImage = record.dynamodb.oldImage != null
        val hasNewImage = record.dynamodb.newImage != null

        if (!hasOldImage && hasNewImage) {
            logger.info { "Found new CREATE event" }
            logger.info { "No actions for create, skipping" }
        } else if (hasOldImage && hasNewImage) {
            logger.info { "Found new MODIFY event" }
            handleModifyEvent(record)
        }
    }

    private fun handleModifyEvent(record: DynamodbEvent.DynamodbStreamRecord) {
        if (record.dynamodb.newImage["pk"]!!.s.startsWith(TransferItem.PK_PREFIX)) {
            logger.info { "PK value: ${record.dynamodb.newImage["pk"]?.s}" }
            val newItem = TransferItem.fromAttributeValueMap(record.dynamodb.newImage)
            val oldItem = TransferItem.fromAttributeValueMap(record.dynamodb.oldImage)
            logger.info { "Got new item $newItem" }
            val webhookUrl = newItem.data!!.webhookUrl
            val status = newItem.status
            val requestId = newItem.requestId
            val transferFulfilled = oldItem.status != newItem.status && newItem.status == TransferStatus.COMPLETED
            if (transferFulfilled && webhookUrl != null) {
                logger.info { "Sending status $status for request id $requestId to webhook $webhookUrl" }
                if (isValidWebhook(webhookUrl)) {
                    webhookUtil.sendTransferStatus(
                        webhookUrl,
                        newItem.data?.merchant?.id!!,
                        newItem.requestId,
                        newItem.status.toApiTransferStatus(),
                        newItem.amount!!
                    )
                } else {
                    logger.warn { "Invalid webhook attempted to publish. Skipping" }
                }
            }
            if (transferFulfilled) {
                websocketUtil.sendWebsocketUpdate(
                    newItem.requestId,
                    newItem.data?.merchant!!.id,
                    newItem.status.toApiTransferStatus()
                )
            }
        } else {
            logger.info { "Item is not a transfer item, skipping" }
        }
    }
}