package com.zenobiapay.cognito.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.cognito.model.RegisterUserRequest
import com.zenobiapay.orum.OrumException
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.Contact
import com.zenobiapay.orum.model.OrumCreatePersonRequest
import com.zenobiapay.table.user.dao.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class RegisterUserOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val orumWrapper: OrumWrapper,
    private val userDao: UserDao,
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String
    ): Any {
        val request = objectMapper.readValue(input.body, RegisterUserRequest::class.java)

        val createPersonRequest = OrumCreatePersonRequest(
            customerReferenceId = request.sub,
            firstName = request.firstName,
            lastName = request.lastName,
            socialSecurityNumber = null,
            contacts = listOf(Contact("email", request.email))
        )

        val person = try {
            orumWrapper.createPerson(createPersonRequest).person.also {
                logger.info { "Created Orum person with customer reference id ${request.sub}" }
            }
        } catch (e: OrumException) {
            if (e.isCreatePersonAlreadyExistsException()) {
                logger.info { "Person ${request.sub} already exists. Updating pre-existing person with new info" }
                orumWrapper.updatePerson(createPersonRequest).person
            } else {
                throw e
            }
        }

        logger.info { "Got person $person" }
        userDao.putUser(request.sub, person.id)
        logger.info { "Successfully put person into table" }
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf()
    }
}