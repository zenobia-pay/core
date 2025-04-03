package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.ListM2MCredentials200Response
import com.zenobiapay.api.generated.models.ListM2MCredentials200ResponseCredentialsInner
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import javax.inject.Inject

class ListM2MCredentialsOperation @Inject constructor(private val userDao: UserDao): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val items = userDao.listM2MCredentials(userId).map {
            ListM2MCredentials200ResponseCredentialsInner(
                it.data.clientId
            )
        }
        return ListM2MCredentials200Response(credentials = items)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}