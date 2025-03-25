package com.zenobiapay.api.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserId(): String? {
    logger.info { "Got authorizer $authorizer and values ${authorizer.keys}" }
    return this.authorizer["sub"] as String
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserPoolGroups(): List<UserPoolGroup> {
    val claims = this.authorizer["claims"] as? Map<String, Any>
    val userGroups = claims?.get("cognito:groups") as String?
    logger.info { "Got claims $claims, userGroups $userGroups" }
    return userGroups?.split(",")?.mapNotNull(UserPoolGroup::fromString) ?: listOf()
}
