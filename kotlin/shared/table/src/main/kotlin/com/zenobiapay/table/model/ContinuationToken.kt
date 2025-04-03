package com.zenobiapay.table.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.Base64

data class ContinuationToken(val key: Map<String, AttributeValue>) {
    companion object {
        fun decodeToken(base64Token: String, objectMapper: ObjectMapper): ContinuationToken {
            val plainTextToken = Base64.getDecoder().decode(base64Token)
            val typeRef = object : TypeReference<Map<String, String>>() {}
            val map = objectMapper.readValue(plainTextToken, typeRef)
            return ContinuationToken(map.mapValues { AttributeValue.fromS(it.value) })
        }
    }

    fun encodeToken(objectMapper: ObjectMapper): String {
        val stringMap = key.mapValues { it.value.s() }
        val json = objectMapper.writeValueAsBytes(stringMap)
        return Base64.getEncoder().encodeToString(json)
    }
}

