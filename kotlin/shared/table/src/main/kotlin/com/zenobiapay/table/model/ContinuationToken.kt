package com.zenobiapay.table.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

class BadTokenException: Exception("Bad token")

data class ContinuationToken(val key: Map<String, AttributeValue>) {
    companion object {
        fun decodeToken(token: String, objectMapper: ObjectMapper, secret: String): ContinuationToken {
            val splitToken = token.split(".")
            if (splitToken.size != 2) {
                logger.info { "Token not formatted separated by one dot" }
                throw BadTokenException()
            }

            val (base64Token, signature) = splitToken
            val expectedSignature = hmacSha256(base64Token, secret)
            if (expectedSignature != signature) {
                logger.info { "Signature is invalid" }
                throw BadTokenException()
            }

            val plainTextToken = Base64.getDecoder().decode(base64Token)
            val typeRef = object : TypeReference<Map<String, String>>() {}
            val map = objectMapper.readValue(plainTextToken, typeRef)
            return ContinuationToken(map.mapValues { AttributeValue.fromS(it.value) })
        }

        private fun hmacSha256(payload: String, secret: String): String {
            val algorithm = "HmacSHA256"
            val secretKeySpec = SecretKeySpec(secret.toByteArray(), algorithm)
            val mac = Mac.getInstance(algorithm)
            mac.init(secretKeySpec)
            val hash = mac.doFinal(payload.toByteArray())
            return Base64.getEncoder().encodeToString(hash)
        }
    }

    fun encodeToken(objectMapper: ObjectMapper, secret: String): String {
        val stringMap = key.mapValues { it.value.s() }
        val json = objectMapper.writeValueAsBytes(stringMap)
        val base64Payload = Base64.getEncoder().encodeToString(json)
        val signature = hmacSha256(base64Payload, secret)
        return "$base64Payload.$signature"
    }
}

