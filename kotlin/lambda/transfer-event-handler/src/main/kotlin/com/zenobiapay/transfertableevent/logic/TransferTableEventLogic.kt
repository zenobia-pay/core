package com.zenobiapay.transfertableevent.logic

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.transfertableevent.util.WebhookUtil
import com.zenobiapay.webhook.util.isValidWebhook
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventLogic @Inject constructor(private val webhookUtil: WebhookUtil) {
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
            logger.info { "Got new item $newItem" }
            val webhookUrl = newItem.data!!.webhookUrl
            val status = newItem.status
            val requestId = newItem.requestId
            if (newItem.transferFulfillId != null && webhookUrl != null) {
                logger.info { "Sending status $status for request id $requestId to webhook $webhookUrl" }
                if (!isValidWebhook(webhookUrl)) {
                    logger.warn { "Invalid webhook attempted to publish. Skipping" }
                    return
                }
                webhookUtil.sendTransferStatus(
                    webhookUrl,
                    newItem.data?.merchant?.id!!,
                    newItem.requestId,
                    newItem.status.toApiTransferStatus(),
                    newItem.amount!!
                )
            }
        } else {
            logger.info { "Item is not a transfer item, skipping" }
        }
    }
}