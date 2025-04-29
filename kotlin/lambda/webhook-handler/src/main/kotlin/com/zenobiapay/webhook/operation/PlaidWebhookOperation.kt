package com.zenobiapay.webhook.operation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.MetricHelper
import com.zenobiapay.api.generated.model.PlaidWebhookRequest
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.webhook.di.WEBHOOK_QUEUE
import com.zenobiapay.webhook.util.decodeJwt
import com.zenobiapay.webhook.util.verifyPlaidJwt
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Named
import software.amazon.awssdk.services.sqs.SqsClient
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class PlaidWebhookOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val plaidWrapper: PlaidWrapper,
    @Named(WEBHOOK_QUEUE) private val webhookQueueUrl: String,
    private val sqsClient: SqsClient,
    private val metricHelper: MetricHelper,
): Operation<PlaidWebhookRequest, Unit>() {
    override val inputType = PlaidWebhookRequest::class.java
    override fun run(
        request: PlaidWebhookRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ) {
        val plaidSignature = input.headers["plaid-verification"] ?: throw InvalidRequestException("Invalid signature")
        val decodedJWT = try {
            decodeJwt(plaidSignature)
        } catch (e: JWTDecodeException) {
            throw InvalidRequestException("Invalid signature")
        }
        if (decodedJWT.algorithm != "ES256") {
            logger.info { "Decoded jwt algorithm ${decodedJWT.algorithm} != ES256"}
            throw InvalidRequestException("Invalid signature")
        }

        val verificationKey = plaidWrapper.getWebhookVerificationKey(decodedJWT.keyId)
        try {
            verifyPlaidJwt(plaidSignature, verificationKey.key.x, verificationKey.key.y)
            logger.info { "Validated signature. Sending to queue." }
            sqsClient.sendMessage {
                it.queueUrl(webhookQueueUrl)
                    .messageBody(objectMapper.writeValueAsString(request))
            }
            metricHelper.putMetric("SendPlaidQueueSuccess", 1.0, mapOf("path" to input.path))
        } catch (e: JWTVerificationException) {
            logger.info { "Plaid jwt was invalid! Not sending to queue." }
            metricHelper.putMetric("InvalidPlaidSignature", 1.0, mapOf("path" to input.path))
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}