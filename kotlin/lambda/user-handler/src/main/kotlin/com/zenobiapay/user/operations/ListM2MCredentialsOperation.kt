package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.ListM2MCredentials200Response
import com.zenobiapay.api.generated.model.ListM2MCredentials200ResponseCredentialsInner
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.table.model.BadTokenException
import com.zenobiapay.table.model.ContinuationToken
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.user.di.UserModule.Companion.PAGINATION_SECRET
import com.zenobiapay.user.model.ListM2mCredentialsRequest
import jakarta.inject.Inject
import jakarta.inject.Named

class ListM2MCredentialsOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val userDao: UserDao,
    @Named(PAGINATION_SECRET) private val paginationSecret: String
): Operation<ListM2mCredentialsRequest, ListM2MCredentials200Response>() {

    override val inputType = ListM2mCredentialsRequest::class.java

    override fun run(
        request: ListM2mCredentialsRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): ListM2MCredentials200Response {
        userId!!
        val request = objectMapper.readValue(input.body, ListM2mCredentialsRequest::class.java)
        val (items, continuationToken) = try {
            userDao.listM2MCredentials(
                userId,
                request.continuationToken,
                paginationSecret
            )
        } catch (e: BadTokenException) {
            throw InvalidRequestException("Bad token")
        }

        return ListM2MCredentials200Response()
            .credentials(items.map {
                ListM2MCredentials200ResponseCredentialsInner()
                    .clientId(it.data.clientId)
            }).continuationToken(continuationToken?.encodeToken(objectMapper, paginationSecret))
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}