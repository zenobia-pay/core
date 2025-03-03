package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.dao.UserDao
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.model.cognito.UserPoolGroup
import com.zenobiapay.model.event.CognitoNewUserEvent
import com.zenobiapay.model.orum.Contact
import com.zenobiapay.model.orum.OrumCreatePersonRequest
import com.zenobiapay.util.CognitoUtil
import com.zenobiapay.util.OrumUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class CognitoEventHandler : RequestHandler<Map<String, Any>, Map<String, Any>> {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var orumUtil: OrumUtil

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var cognitoUtil: CognitoUtil

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: Map<String, Any>, context: Context?): Map<String, Any> {
        try {
            logger.info { "Got event $event" }
            val userEvent = CognitoNewUserEvent.from(event, objectMapper)
            logger.info { "Got event $userEvent" }
            val sub = userEvent.userName

            cognitoUtil.addUserToGroup(sub, UserPoolGroup.CUSTOMER)

            val person = orumUtil.createPerson(
                OrumCreatePersonRequest(
                    customerReferenceId = sub,
                    firstName = userEvent.request.userAttributes.givenName,
                    lastName = userEvent.request.userAttributes.familyName,
                    socialSecurityNumber = null,
                    contacts = listOf(Contact("email", userEvent.request.userAttributes.email))
                )
            )
            logger.info { "Got person $person" }
            userDao.putUser(sub, person.person.id)
            logger.info { "Successfully put person into table" }
        } catch (e: Exception) {
            logger.error(e) { "Error when processing cognito event" }
        }

        return event
    }
}