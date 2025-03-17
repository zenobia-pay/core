package com.zenobiapay.cognito.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.cognito.CognitoUtil
import com.zenobiapay.orum.OrumException
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.cognito.di.DaggerAppComponent
import com.zenobiapay.orum.model.Contact
import com.zenobiapay.orum.model.OrumCreatePersonRequest
import com.zenobiapay.model.event.CognitoNewUserEvent
import com.zenobiapay.table.user.dao.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

// TODO: use cognito event object
class CognitoEventHandler : RequestHandler<Map<String, Any>, Map<String, Any>> {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var orumWrapper: OrumWrapper

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var cognitoUtil: CognitoUtil

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: Map<String, Any>, context: Context?): Map<String, Any> {
        try {
            // TODO: refactor to use AWS cognito event object
            logger.info { "Got event $event" }
            val userEvent = CognitoNewUserEvent.from(event, objectMapper)
            logger.info { "Got event $userEvent" }
            val sub = userEvent.userName

            cognitoUtil.addUserToGroup(sub, UserPoolGroup.CUSTOMER)

            val createPersonRequest = OrumCreatePersonRequest(
                customerReferenceId = sub,
                firstName = userEvent.request.userAttributes.givenName,
                lastName = userEvent.request.userAttributes.familyName,
                socialSecurityNumber = null,
                contacts = listOf(Contact("email", userEvent.request.userAttributes.email))
            )

            val person = try {
                orumWrapper.createPerson(createPersonRequest).person.also {
                    logger.info { "Created Orum person with customer reference id $sub" }
                }
            } catch (e: OrumException) {
                if (e.isCreatePersonAlreadyExistsException()) {
                    logger.info { "Person $sub already exists. Updating pre-existing person with new info" }
                    orumWrapper.updatePerson(createPersonRequest).person
                } else {
                    throw e
                }
            }

            logger.info { "Got person $person" }
            userDao.putUser(sub, person.id)
            logger.info { "Successfully put person into table" }
        } catch (e: Exception) {
            logger.error(e) { "Error when processing cognito event" }
        }

        return event
    }
}