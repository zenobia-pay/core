package com.zenobiapay.webhook.operation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.OrumWebhookRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.table.transfer.dao.PAYOUT_PREFIX
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.webhook.di.ORUM_PUBLIC_CERTIFICATE
import com.zenobiapay.webhook.model.BusinessEventBody
import com.zenobiapay.webhook.model.ExternalAccount
import com.zenobiapay.webhook.model.TransferEventBody
import com.zenobiapay.webhook.util.SlackChannel
import com.zenobiapay.webhook.util.SlackUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

private val logger = KotlinLogging.logger {}

class OrumWebhookOperation @Inject constructor(
    @Named(ORUM_PUBLIC_CERTIFICATE) private val orumPublicCertificate: String,
    private val objectMapper: ObjectMapper,
    private val slackUtil: SlackUtil,
    private val transferDao: TransferDao,
): Operation<OrumWebhookRequest, EmptyApiResponse>() {
    override val inputType = OrumWebhookRequest::class.java
    override fun run(
        request: OrumWebhookRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        logger.debug { "Got request $request, body ${input.body}, headers ${input.headers}" }
        if (!isSignatureValid(request, input)) {
            logger.info { "Signature did not match. Failing" }
            throw InvalidRequestException("Invalid signature")
        }
        if (request.eventType == "transfer_updated") {
            handleTransferEvent(request)
        }

        val message = getMessage(request)
        if (message != null) {
            logger.info { "Sending slack message $message" }
            slackUtil.sendMessage(message, SlackChannel.ORUM)
            logger.info { "Successfully sent slack message" }
        }
        return EmptyApiResponse()
    }

    private fun handleTransferEvent(request: OrumWebhookRequest) {
        logger.info { "Transfer request found. Seeing if request item needs an update." }
        val data = request.eventData as Map<String, Any>
        val stringData = objectMapper.writeValueAsString(data)
        val event = objectMapper.readValue(stringData, TransferEventBody::class.java).transfer
        if (event.transfer_reference_id.startsWith(PAYOUT_PREFIX)) {
            logger.info { "Transfer is a payout. Skipping publishing updates" }
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
        if (status == "failed") {
            return """
                *Transfer Failed!* <@channel>
                - `${event.transfer_reference_id}` failed:
                - Source: `${event.source}`
                - Destination: `${event.destination}`
            """.trimIndent()
        } else {
            logger.info { "Got status $status, skipping publishing slack message" }
            return null
        }
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

    private fun isSignatureValid(request: OrumWebhookRequest, input: APIGatewayProxyRequestEvent): Boolean {
        val body = input.body
        val signature = input.headers["Signature"]
        val messagePlusCreatedAt = body + request.createdAt

        val certificate = String(Base64.getDecoder().decode(orumPublicCertificate), Charsets.UTF_8)

        val trimmedCertificate = certificate.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val publicKeyBytes = Base64.getDecoder().decode(trimmedCertificate)
        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val  publicKey = keyFactory.generatePublic(publicKeySpec)
        val digest = messagePlusCreatedAt.toByteArray(StandardCharsets.UTF_8)
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(digest)
        return sig.verify(Base64.getDecoder().decode(signature))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}