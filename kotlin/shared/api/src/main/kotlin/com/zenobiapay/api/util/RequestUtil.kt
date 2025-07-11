package com.zenobiapay.api.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserId(): String? {
    com.zenobiapay.api.util.logger.info { "Got authorizer $authorizer" }
    return this.authorizer["sub"] as String?
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getEmail(): String? {
    return this.authorizer["email"] as String?
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserRoles(): List<UserPoolGroup> {
    val roles = this.authorizer["roles"] as List<String>?
    return roles?.map { UserPoolGroup.fromString(it) } ?: listOf(UserPoolGroup.UNKNOWN)
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getSubForM2M(): String? {
    return this.authorizer["m2mSub"] as String?
}
