package com.zenobiapay.m2mhandler.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.auth0.client.mgmt.ManagementAPI
import com.auth0.json.mgmt.client.Client
import com.zenobiapay.api.generated.models.CreateM2mCredentials200Response
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.m2mhandler.model.Auth0Exception
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class CreateM2MCredentialsOperation @Inject constructor(private val managementAPI: ManagementAPI): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val client = Client("${userId}_${UUID.randomUUID()}")
        client.description = "M2M Client to act on behalf of merchant $userId"
        client.appType = "non_interactive"
        client.clientMetadata = mapOf("merchantSub" to userId)

        val createClientResponse = managementAPI.clients().create(client).execute()
        if (createClientResponse.statusCode >= 400) { // Auth0 returns 201 instead of 200
            logger.error { "Got error code ${createClientResponse.statusCode}, ${createClientResponse.body}"}
            throw Auth0Exception("Failed to create new m2m client")
        }

        logger.info { "Successfully created m2m client with name ${createClientResponse.body.name}" }
        return CreateM2mCredentials200Response(
            clientId = createClientResponse.body.clientId,
            clientSecret = createClientResponse.body.clientSecret
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
