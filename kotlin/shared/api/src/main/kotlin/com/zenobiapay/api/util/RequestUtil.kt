package com.zenobiapay.api.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.UserType
import com.zenobiapay.api.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserId(): String? {
    logger.info { "Got authorizer $authorizer" }
    return this.authorizer["sub"] as String?
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getEmail(): String? {
    return this.authorizer["email"] as String?
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getUserRole(): UserPoolGroup {
    val role = this.authorizer["role"] as String?
    return UserPoolGroup.fromString(role)
}

fun APIGatewayProxyRequestEvent.ProxyRequestContext.getSubForM2M(): String? {
    return this.authorizer["m2mSub"] as String?
}
