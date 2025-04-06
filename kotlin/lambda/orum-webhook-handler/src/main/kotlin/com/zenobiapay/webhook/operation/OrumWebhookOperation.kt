package com.zenobiapay.webhook.operation

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.OrumWebhookRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.webhook.di.ORUM_PUBLIC_CERTIFICATE
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

private val logger = KotlinLogging.logger {}

class OrumWebhookOperation @Inject constructor(
    @Named(ORUM_PUBLIC_CERTIFICATE) private val orumPublicCertificate: String,
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
        logger.info { "Signature matched!" }
        return EmptyApiResponse()
    }

    private fun isSignatureValid(request: OrumWebhookRequest, input: APIGatewayProxyRequestEvent): Boolean {
        val body = input.body
        val signature = input.headers["Signature"]
        val messagePlusCreatedAt = body + request.createdAt

        val certificate = String(Base64.getDecoder().decode(orumPublicCertificate), Charsets.UTF_8)
        logger.info { "Got certificate $certificate" }

        val publicKeyBytes = Base64.getDecoder().decode(certificate);
        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes);
        val keyFactory = KeyFactory.getInstance("RSA");
        val  publicKey = keyFactory.generatePublic(publicKeySpec);
        val digest = messagePlusCreatedAt.toByteArray(StandardCharsets.UTF_8);
        val sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(digest);
        return sig.verify(Base64.getDecoder().decode(signature));
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}