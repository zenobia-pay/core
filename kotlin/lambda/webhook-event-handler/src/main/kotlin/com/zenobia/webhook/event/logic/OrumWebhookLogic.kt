package com.zenobia.webhook.event.logic

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.MetricHelper
import com.zenobia.webhook.event.model.BusinessEventBody
import com.zenobia.webhook.event.model.ExternalAccount
import com.zenobia.webhook.event.model.TransferEventBody
import com.zenobia.webhook.event.util.SlackChannel
import com.zenobia.webhook.event.util.SlackUtil
import com.zenobiapay.api.generated.model.OrumWebhookRequest
import com.zenobiapay.table.transfer.dao.PAYOUT_PREFIX
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class OrumWebhookLogic @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val slackUtil: SlackUtil,
    private val transferDao: TransferDao,
    private val metricHelper: MetricHelper,
) {
    fun handleRequest(request: OrumWebhookRequest) {
        if (request.eventType == "transfer_updated") {
            metricHelper.emitSuccessMetric("HandleTransferEventSuccess", mapOf()) {
                handleTransferEvent(request)
            }
        }

        val message = getMessage(request)
        if (message != null) {
            metricHelper.emitSuccessMetric("SendSlackMessageSuccess", mapOf()) {
                logger.info { "Sending slack message $message" }
                slackUtil.sendMessage(message, SlackChannel.ORUM)
                logger.info { "Successfully sent slack message" }
            }
        }
    }

    private fun handleTransferEvent(request: OrumWebhookRequest) {
        logger.info { "Transfer request found. Seeing if request item needs an update." }
        val data = request.eventData as Map<String, Any>
        val stringData = objectMapper.writeValueAsString(data)
        val event = objectMapper.readValue(stringData, TransferEventBody::class.java).transfer
        if (event.transfer_reference_id.startsWith(PAYOUT_PREFIX)) {
            logger.info { "Transfer is a payout. Skipping publishing updates" }
            return
        }
        val transferItem = transferDao.getTransfer(event.transfer_reference_id) ?: throw Exception("Could not find transfer request ${event.transfer_reference_id}")

        val inboundStatus = InboundTransferStatus.fromOrumTransferStatus(event.status!!)
        if (inboundStatus == InboundTransferStatus.NOT_STARTED || inboundStatus == InboundTransferStatus.IN_FLIGHT) {
            logger.info { "Ignoring updating status, status is either not started or in flight." }
            return
        }

        if (transferItem.inboundStatus.order >= inboundStatus.order) {
            logger.info { "transfer item has later status ${transferItem.inboundStatus.order}, skipping update $inboundStatus"}
            return
        }
        logger.info { "Updating inbound status $inboundStatus" }
        transferDao.updateTransferInboundStatus(transferItem, inboundStatus)
    }

    private fun getMessage(request: OrumWebhookRequest): String? {
        val data = request.eventData as Map<String, Any>
        val stringData = objectMapper.writeValueAsString(data)
        return when (request.eventType) {
            "transfer_updated" -> handleTransferUpdated(stringData)
            "business_rejected", "business_restricted", "business_created", "business_verified" -> handleBusinessUpdated(stringData, request.eventType)
            "external_account_rejected", "external_account_restricted", "verify_account_updated" -> handleAccount(stringData, request.eventType)
            else -> null.also { logger.warn { "Unrecognized event ${request.eventType}" } }
        }
    }

    private fun handleTransferUpdated(data: String): String? {
        val event = objectMapper.readValue(data, TransferEventBody::class.java).transfer
        val status = event.status
        return """
            *Transfer Update ${status}* <@channel>
            - Id: `${event.transfer_reference_id}`:
            - Source: `${event.source}`
            - Destination: `${event.destination}`
        """.trimIndent()
    }

    private fun handleBusinessUpdated(data: String, eventType: String): String {
        val event = objectMapper.readValue(data, BusinessEventBody::class.java).business
        return """
            *Business Update - ${event.entity_name}*
            - ID: ${event.customer_reference_id}
            - Status: `${eventType}`
        """.trimIndent()
    }

    private fun handleAccount(data: String, eventType: String): String {
        val event = objectMapper.readValue(data, ExternalAccount::class.java).external_account
        return """
            *Account Update - Status ${eventType}*
            - Account ref id: `${event.account_reference_id}`
            - Account type: `${event.account_type}`
            - Customer ref id: `${event.customer_reference_id}`
            - Owner type: `${event.customer_resource_type}`
        """.trimIndent()
    }
}