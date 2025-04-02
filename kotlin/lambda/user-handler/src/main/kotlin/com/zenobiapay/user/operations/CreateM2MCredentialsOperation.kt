package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.CreateM2mCredentials200Response
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.user.util.Auth0Wrapper
import com.zenobiapay.user.util.Auth0Wrapper.Companion.ZENOBIA_AUDIENCE
import javax.inject.Inject

class CreateM2MCredentialsOperation @Inject constructor(private val auth0Wrapper: Auth0Wrapper): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val createdClient = auth0Wrapper.createClientCredentials(userId)
        auth0Wrapper.createClientGrant(createdClient, ZENOBIA_AUDIENCE)

        return CreateM2mCredentials200Response(
            clientId = createdClient.clientId,
            clientSecret = createdClient.clientSecret
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}