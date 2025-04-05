package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListM2MCredentials200Response
import com.zenobiapay.api.generated.model.ListM2MCredentials200ResponseCredentialsInner
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.user.model.ListM2mCredentialsRequest
import javax.inject.Inject

class ListM2MCredentialsOperation @Inject constructor(private val objectMapper: ObjectMapper, private val userDao: UserDao): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val request = objectMapper.readValue(input.body, ListM2mCredentialsRequest::class.java)
        val (items, continuationToken) = userDao.listM2MCredentials(userId, request.continuationToken)

        return ListM2MCredentials200Response()
            .credentials(items.map {
                ListM2MCredentials200ResponseCredentialsInner()
                    .clientId(it.data.clientId)
            }).continuationToken(continuationToken?.encodeToken(objectMapper))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}