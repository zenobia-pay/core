package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.CreateM2mCredentials200Response
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.user.util.Auth0Wrapper
import com.zenobiapay.user.util.Auth0Wrapper.Companion.ZENOBIA_AUDIENCE
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class CreateM2MCredentialsOperation @Inject constructor(private val auth0Wrapper: Auth0Wrapper, private val userDao: UserDao): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val client = auth0Wrapper.createClientCredentials(userId)
        logger.info { "Created client credentials for user id $userId" }
        auth0Wrapper.createClientGrant(client, ZENOBIA_AUDIENCE)

        userDao.putM2MCredentials(userId, client.clientId, client.name)
        logger.info { "Recorded m2m credentials in ddb table"}

        return CreateM2mCredentials200Response(
            clientId = client.clientId,
            clientSecret = client.clientSecret
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}