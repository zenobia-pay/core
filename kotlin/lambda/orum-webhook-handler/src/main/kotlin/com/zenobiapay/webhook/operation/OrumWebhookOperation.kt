package com.zenobiapay.webhook.operation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.OrumWebhookRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
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
): Operation<OrumWebhookRequest, EmptyApiResponse>() {
    override val inputType = OrumWebhookRequest::class.java
    override fun run(
        request: OrumWebhookRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        logger.info { "Got request $request, body ${input.body}, headers ${input.headers}" }
        if (!isSignatureValid(request, input)) {
            logger.info { "Signature did not match. Failing" }
            throw InvalidRequestException("Invalid signature")
        }
        val message = getMessage(request)
        if (message != null) {
            slackUtil.sendMessage(message, SlackChannel.ORUM)
        }
        return EmptyApiResponse()
    }

    private fun getMessage(request: OrumWebhookRequest): String? {
        val data = request.eventData as Map<String, Any>
        val stringData = objectMapper.writeValueAsString(data)
        logger.info { "Got string data $stringData" }
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
            logger.info { "Got status $status, skipping publishing" }
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

        logger.info { "Using base64 certificate $orumPublicCertificate" }
        val certificate = String(Base64.getDecoder().decode(orumPublicCertificate), Charsets.UTF_8)
        logger.info { "Got certificate $certificate" }

        val trimmedCertificate = certificate.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        logger.info { "Got trimmed certificate $trimmedCertificate" }

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