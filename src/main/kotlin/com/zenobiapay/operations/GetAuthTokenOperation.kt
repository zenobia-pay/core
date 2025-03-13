package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.CredentialsDao
import com.zenobiapay.model.api.credentials.GetAuthTokenRequest
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.util.OAuthUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class GetAuthTokenOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val credentialsDao: CredentialsDao,
    private val oAuthUtil: OAuthUtil
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String
    ): Any {
        val request = objectMapper.readValue(input.body, GetAuthTokenRequest::class.java)
        val credentials = credentialsDao.getCredentials(request.clientId)
        return 1
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        TODO("Not yet implemented")
    }
}