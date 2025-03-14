package com.zenobiapay.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoSubFoundException(e: String) : Exception(e)

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserId(objectMapper: ObjectMapper): String {
    val claims = this.authorizer["claims"]
    val claimsMap = objectMapper.convertValue(claims, object : TypeReference<Map<String, String>>() {})

    return claimsMap["sub"] ?: throw NoSubFoundException("Could not find sub key in claims map")
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserPoolGroups(): List<UserPoolGroup> {
    val claims = this.authorizer["claims"] as? Map<String, Any>
    val userGroups = claims?.get("cognito:groups") as String?
    logger.info { "Got claims $claims, userGroups $userGroups" }
    return userGroups?.split(",")?.mapNotNull(UserPoolGroup::fromString) ?: listOf()
}
