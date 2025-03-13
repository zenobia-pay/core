package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.dao.CredentialsDao
import com.zenobiapay.model.api.credentials.GenerateSecretsResponse
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.util.CognitoUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class GenerateSecretsOperation @Inject constructor(
    private val credentialsDao: CredentialsDao,
    private val cognitoUtil: CognitoUtil,
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String
    ): Any {
        val clientName = "${userId}_${UUID.randomUUID()}"
        logger.info { "Generated new client name $clientName" }
        val userPoolClient = cognitoUtil.createUserPoolClient(clientName)
        credentialsDao.putCredentials(clientName, userPoolClient.clientId(), userId)
        return GenerateSecretsResponse(
            clientId = userPoolClient.clientId(),
            clientSecret = userPoolClient.clientSecret(),
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}