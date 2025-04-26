package com.zenobiapay.transfertableevent.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.transfertableevent.di.WEBHOOK_KMS_ALIAS
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.SignRequest
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import jakarta.inject.Inject
import jakarta.inject.Named

private val logger = KotlinLogging.logger {}

class JwtUtil @Inject constructor(
    private val kmsClient: KmsClient,
    @Named(WEBHOOK_KMS_ALIAS) private val webhookAlias: String,
    private val objectMapper: ObjectMapper
) {
    fun signJwtWithKms(body: Map<String, Any?>, sub: String): String {
        val signingAlgorithm = SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"RS256","typ":"JWT"}""".toByteArray())

        val bodyWithExpiry = body + mapOf(
            "sub" to sub,
            "iat" to Instant.now().epochSecond,
            "exp" to Instant.now().plus(Duration.ofMinutes(5)).epochSecond,
        )

        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(objectMapper.writeValueAsBytes(bodyWithExpiry))

        val message = "$header.$payload".toByteArray()

        // Sign with KMS
        val signResponse = kmsClient.sign(
            SignRequest.builder()
                .keyId(webhookAlias)
                .signingAlgorithm(signingAlgorithm)
                .message(SdkBytes.fromByteArray(message))
                .messageType("RAW")
                .build()
        )

        val signature = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(signResponse.signature().asByteArray())

        return "$header.$payload.$signature".also {
            logger.info { "Got jwt $it" }
        }
    }
}
