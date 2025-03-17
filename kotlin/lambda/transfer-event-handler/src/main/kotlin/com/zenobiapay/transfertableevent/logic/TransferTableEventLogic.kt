package com.zenobiapay.transfertableevent.logic

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.util.WebhookHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventLogic @Inject constructor(private val webhookHandler: WebhookHandler) {
    fun handleRecord(record: DynamodbEvent.DynamodbStreamRecord) {
        val hasOldImage = record.dynamodb.oldImage != null
        val hasNewImage = record.dynamodb.newImage != null

        if (!hasOldImage && hasNewImage) {
            logger.info { "Found new CREATE event" }
        } else if (hasOldImage && hasNewImage) {
            logger.info { "Found new MODIFY event" }
            handleModifyEvent(record)
        }
    }

    private fun handleModifyEvent(record: DynamodbEvent.DynamodbStreamRecord) {
        if (record.dynamodb.newImage["pk"]!!.s.startsWith(TransferItem.PK_PREFIX)) {
            logger.info { "PK value: ${record.dynamodb.newImage["pk"]?.s}" }
            val newItem = TransferItem.fromAttributeValueMap(record.dynamodb.newImage)
            val webhookUrl = newItem.data!!.webhookUrl
            val status = newItem.status
            val requestId = newItem.requestId
            if (newItem.transferFulfillId != null && webhookUrl != null) {
                logger.info { "Sending status $status for request id $requestId to webhook $webhookUrl" }
                webhookHandler.sendTransferStatus(
                    webhookUrl,
                    newItem.requestId,
                    newItem.status.toApiTransferStatus(),
                    newItem.amount!!
                )
            }
            logger.info { "Got new item $newItem" }
        } else {
            logger.info { "Item is not a transfer item, skipping" }
        }
    }
}