package com.zenobiapay.api.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserId(objectMapper: ObjectMapper): String? {
    logger.info { "Got authorizer $authorizer and values ${authorizer.keys}" }
    val claims = this.authorizer["claims"]

    if (claims == null) return null

    val claimsMap = objectMapper.convertValue(claims, object : TypeReference<Map<String, String>>() {})

    return claimsMap["sub"]
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserPoolGroups(): List<UserPoolGroup> {
    val claims = this.authorizer["claims"] as? Map<String, Any>
    val userGroups = claims?.get("cognito:groups") as String?
    logger.info { "Got claims $claims, userGroups $userGroups" }
    return userGroups?.split(",")?.mapNotNull(UserPoolGroup::fromString) ?: listOf()
}
