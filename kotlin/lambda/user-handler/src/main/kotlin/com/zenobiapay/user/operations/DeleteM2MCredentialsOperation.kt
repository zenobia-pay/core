package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.generated.model.DeleteM2MCredentialsRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.user.util.Auth0Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class DeleteM2MCredentialsOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val userDao: UserDao,
    private val auth0Wrapper: Auth0Wrapper,
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val request = objectMapper.readValue(input.body, DeleteM2MCredentialsRequest::class.java)
        userDao.getM2MCredentials(userId, request.clientId) ?: throw ResourceNotFoundException("M2M_CLIENT_ID")

        logger.info { "Attempting to delete using auth0 management"}
        auth0Wrapper.deleteClientCredentials(request.clientId)

        logger.info { "Deleting m2m credentials from ddb table" }
        userDao.deleteM2MCredentials(userId, request.clientId)

        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}